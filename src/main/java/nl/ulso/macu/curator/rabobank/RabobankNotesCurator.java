package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.curator.CuratorTemplate;
import nl.ulso.macu.curator.common.omnifocus.OmniFocusQuery;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;

public class RabobankNotesCurator
        extends CuratorTemplate
{
    private Journal journal;
    private OrgChart orgChart;

    @Override
    protected FileSystemVault createVault()
            throws IOException
    {
        return createVaultForPathInUserHome("Notes", "Rabobank");
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, Vault vault)
    {
        catalog.register(new OmniFocusQuery(vault));
        catalog.register(new ProjectsQuery(vault));
        catalog.register(new ArticlesQuery(vault));
        catalog.register(new SystemsQuery(vault));
        catalog.register(new ArchitectureDecisionRecordsQuery(vault));
        catalog.register(new TeamQuery(vault));
        catalog.register(new OneOnOneQuery(vault));
        journal = new Journal(vault);
        catalog.register(new WeeklyQuery(journal));
        orgChart = new OrgChart(vault);
        catalog.register(new DepartmentsQuery(orgChart));
        catalog.register(new RolesQuery(orgChart));
    }

    @Override
    public void vaultChanged()
    {
        journal.refresh();
        orgChart.refresh();
        super.vaultChanged();
    }

    public static void main(String[] args)
    {
        new RabobankNotesCurator().runOnce();
    }
}
