package nl.ulso.markdown_curator.curator.rabobank;

import nl.ulso.markdown_curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

public class OrgChart
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OrgChart.class);
    public static final String TEAMS_FOLDER = "Teams";
    public static final String CONTACTS_FOLDER = "Contacts";

    private final Vault vault;

    private final Set<OrgUnit> orgUnits;

    public OrgChart(Vault vault)
    {
        this.vault = vault;
        this.orgUnits = new HashSet<>();
    }

    void refresh()
    {
        orgUnits.clear();
        vault.folder(TEAMS_FOLDER).ifPresent(teams -> {
            vault.folder(CONTACTS_FOLDER).ifPresent(contacts -> {
                teams.documents().forEach(team ->
                {
                    team.accept(new OrgUnitFinder(teams, contacts));
                });
            });
        });
    }

    List<OrgUnit> forParent(String parentTeamName)
    {
        return orgUnits.stream()
                .filter(orgUnit -> orgUnit.parent().name().contentEquals(parentTeamName))
                .toList();
    }

    List<OrgUnit> forContact(String contactName)
    {
        return orgUnits.stream()
                .filter(orgUnit -> orgUnit.leadership().values().stream()
                        .anyMatch(document -> document.name().contentEquals(contactName)))
                .toList();
    }


    private class OrgUnitFinder
            extends BreadthFirstVaultVisitor
    {
        private static final String ROLES_SECTION = "Roles";
        private static final String ROLE_PATTERN_START = "^- (.*?): ";
        private final Folder teams;
        private final Folder contacts;

        private Document parent;
        private Map<String, Document> leadership;

        OrgUnitFinder(Folder teams, Folder contacts)
        {
            this.teams = teams;
            this.contacts = contacts;
            this.parent = null;
        }

        @Override
        public void visit(Document document)
        {
            parent = null;
            if (document.fragments().size() > 2
                    && document.fragments().get(1) instanceof TextBlock textBlock)
            {
                parent = textBlock.findInternalLinks().stream()
                        .map(InternalLink::targetDocument)
                        .map(teams::document)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                        .orElse(null);
            }
            if (parent != null)
            {
                leadership = new HashMap<>();
                super.visit(document);
                orgUnits.add(new OrgUnit(document, parent, leadership));
            }
        }

        @Override
        public void visit(Section section)
        {
            if (section.level() == 2
                    && section.title().contentEquals(ROLES_SECTION))
            {
                super.visit(section);
            }
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            if (parent == null)
            {
                return;
            }

            String content = textBlock.content();
            textBlock.findInternalLinks().stream()
                    .filter(link -> contacts.document(link.targetDocument()).isPresent())
                    .forEach(link -> {
                        var regex = compile(ROLE_PATTERN_START + quote(link.toMarkdown()), Pattern.MULTILINE);
                        var matcher = regex.matcher(content);
                        if (matcher.find())
                        {
                            leadership.put(matcher.group(1),
                                    contacts.document(link.targetDocument()).orElseThrow());
                        }
                    });
        }
    }
}
