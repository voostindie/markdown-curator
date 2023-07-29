package nl.ulso.markdown_curator.query;

import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.ResourceBundle.getBundle;

public class ResourceBundledGeneralMessages
        implements GeneralMessages
{
    private final ResourceBundle bundle;

    public ResourceBundledGeneralMessages()
    {
        this(Locale.ENGLISH);
    }

    @Inject
    public ResourceBundledGeneralMessages(Locale locale)
    {
        this.bundle = getBundle("GeneralMessages", locale);
    }

    @Override
    public String noResults()
    {
        return bundle.getString("results.zero");
    }

    @Override
    public String resultSummary(int resultCount)
    {
        if (resultCount == 0)
        {
            return noResults();
        }
        else if (resultCount == 1)
        {
            return bundle.getString("results.one");
        }
        else
        {
            return MessageFormat.format(bundle.getString("results.many"), resultCount);
        }
    }

    @Override
    public String performanceWarning()
    {
        return bundle.getString("results.performance_warning");
    }
}
