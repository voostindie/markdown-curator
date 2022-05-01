package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.ZoneId.systemDefault;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static nl.ulso.macu.query.QueryResult.failure;
import static nl.ulso.macu.query.QueryResult.table;

class OneOnOneQuery
        implements Query
{
    private final Vault vault;

    OneOnOneQuery(Vault vault) {this.vault = vault;}

    @Override
    public String name()
    {
        return "1on1";
    }

    @Override
    public String description()
    {
        return "outputs an regular 1-on-1s";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return vault.folder("Contacts").map(folder -> {
            var finder = new OneOnOneFinder();
            folder.accept(finder);
            var contacts = finder.contacts;
            contacts.sort(comparing((Map<String, String> e) -> e.get("Date")));
            return table(List.of("Date", "Name", "Weeks ago"), contacts);
        }).orElse(failure("Couldn't find the folder 'Contacts'"));
    }

    private static class OneOnOneFinder
            extends BreadthFirstVaultVisitor
    {
        public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        private final List<Map<String, String>> contacts = new ArrayList<>();

        @Override
        public void visit(Document document)
        {
            var date = document.frontMatter().date("1-on-1", null);
            if (date != null)
            {
                contacts.add(Map.of(
                        "Date", DATE_FORMAT.format(date),
                        "Name", document.link(),
                        "Weeks ago", computeWeeksAgo(date))
                );
            }
        }

        private String computeWeeksAgo(Date date)
        {
            var today = new Date();
            if (date.after(today))
            {
                return "Planned";
            }
            var startDate = LocalDateTime.ofInstant(date.toInstant(), systemDefault());
            var endDate = LocalDateTime.ofInstant(today.toInstant(), systemDefault());
            return Long.toString(ChronoUnit.WEEKS.between(startDate, endDate));
        }
    }
}