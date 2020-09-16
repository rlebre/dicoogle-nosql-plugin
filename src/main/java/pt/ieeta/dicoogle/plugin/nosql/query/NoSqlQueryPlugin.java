package pt.ieeta.dicoogle.plugin.nosql.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseMiddleware;
import pt.ieeta.dicoogle.plugin.nosql.database.MongoUtil;
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
    private DatabaseMiddleware databaseMiddleware;

    public NoSqlQueryPlugin(DatabaseMiddleware databaseMiddleware) {
        this.enabled = true;
        this.databaseMiddleware = databaseMiddleware;
    }

    public static Map<String, Object> getDefaultExtraFields() {
        Map<String, Object> extrafields = new HashMap<>();
        extrafields.put("SOPInstanceUID", "SOPInstanceUID");
        extrafields.put("URI", "URI");

        return extrafields;
    }

    @Override
    public Iterable<SearchResult> query(String query, Object... parameters) {
        if (this.databaseMiddleware == null) {
            logger.warn("Query was attempted before settings were initialized");
            return Collections.EMPTY_LIST;
        }

        List<SearchResult> results = new ArrayList<>();

        List<HashMap<String, Object>> result = new ArrayList<>();
        Map<String, Object> extraFields = getDefaultExtraFields();

        long startTime = System.currentTimeMillis();

        if (parameters.length > 0) {
            extraFields = (HashMap<String, Object>) parameters[0];
        }

        result.addAll(this.databaseMiddleware.find(MongoUtil.parseStringToQuery(query), extraFields));

        for (HashMap<String, Object> map : result) {
            SearchResult r = new SearchResult(URI.create((String) map.get("URI")), 1, map);
            results.add(r);
        }

        long stopTime = System.currentTimeMillis();
        logger.info("Finished opening result stream, Query size: {}, Elapsed: {} ms, Query: {}", results.size(), (stopTime - startTime), query);

        return results;
    }

    /**
     * This method is used to retrieve the unique name of the indexer.
     *
     * @return a fixed name for the indexer
     */
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

    /**
     * Sets the database interface after the plugin is initialized
     *
     * @param databaseMiddleware Database interface to set
     */
    public void setDatabaseInterface(DatabaseMiddleware databaseMiddleware) {
        this.databaseMiddleware = databaseMiddleware;
    }
}
