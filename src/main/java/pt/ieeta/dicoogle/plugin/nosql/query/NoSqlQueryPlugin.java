package pt.ieeta.dicoogle.plugin.nosql.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseInterface;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Query provider for Distributed NoSQL plugin.
 *
 * @author Rui Lebre, ruilebre@ua.pt
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
public class NoSqlQueryPlugin implements QueryInterface {
    private static final Logger logger = LoggerFactory.getLogger(NoSqlQueryPlugin.class);
    private static final Pattern pattern = Pattern.compile("([a-zA-Z_0-9]*:(Float|Numeric):)+");
    private boolean enabled;
    private ConfigurationHolder settings;
    private DatabaseInterface databaseInterface;

    public NoSqlQueryPlugin(DatabaseInterface databaseInterface) {
        this.enabled = true;
        this.databaseInterface = databaseInterface;
    }

    public static Map<String, Object> getDefaultExtraFields() {
        Map<String, Object> extrafields = new HashMap<>();
        extrafields.put("SOPInstanceUID", "SOPInstanceUID");
        extrafields.put("URI", "URI");

        return extrafields;
    }

    @Override
    public Iterable<SearchResult> query(String query, Object... parameters) {
        if (this.databaseInterface == null) {
            logger.warn("Query was attempted before settings were initialized");
            return Collections.EMPTY_LIST;
        }

        query = query.replace("Float:", "");
        query = query.replace("Numeric:", "");
        query = query.replaceAll("\"", "");
        query = query.replaceAll("\\(", "").replaceAll("\\)", "");

        // TODO: Support complex queries with OR and AND
        String[] terms = query.split("OR |AND ");

        List<SearchResult> results = new ArrayList<>();

        List<HashMap<String, Object>> result = new ArrayList<>();
        Map<String, Object> extrafields = getDefaultExtraFields();
        long startTime = System.currentTimeMillis();

        if (parameters.length > 0) {
            extrafields = (HashMap<String, Object>) parameters[0];
        }

        for (String term : terms) {
            String[] tagValue = term.split(":");
            result.addAll(this.databaseInterface.find(tagValue[0], tagValue[1], extrafields));
        }

        for (HashMap<String, Object> map : result) {
            SearchResult r = new SearchResult(URI.create((String) map.get("URI")), 1, map);
            results.add(r);
        }

        long stopTime = System.currentTimeMillis();
        logger.info("Finished opening result stream, Query: {},{},{}", results.size(), (stopTime - startTime), query);

        return results;
    }

    @Override
    public String getName() {
        return "dicoogle-nosql-query";
    }

    @Override
    public boolean enable() {
        this.enabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.enabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
    }

    public void setDatabaseInterface(DatabaseInterface databaseInterface) {
        this.databaseInterface = databaseInterface;
    }
}
