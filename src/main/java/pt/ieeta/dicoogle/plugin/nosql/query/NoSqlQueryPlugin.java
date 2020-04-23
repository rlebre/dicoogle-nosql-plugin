package pt.ieeta.dicoogle.plugin.nosql.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseInterface;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
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
    private boolean enabled;
    private ConfigurationHolder settings;
    private DatabaseInterface databaseInterface;
    private static final Pattern pattern = Pattern.compile("([a-zA-Z_0-9]*:(Float|Numeric):)+");

    public NoSqlQueryPlugin(DatabaseInterface databaseInterface) {
        this.enabled = true;
        this.databaseInterface = databaseInterface;
    }

    @Override
    public Iterable<SearchResult> query(String query, Object... parameters) {
        long time = System.currentTimeMillis();

        if (this.databaseInterface == null) {
            logger.warn("Query was attempted before settings were initialized");
            return Collections.EMPTY_LIST;
        }

        Matcher matcher = pattern.matcher(query);
        ArrayList<String> fieldsNumeric = new ArrayList<>();
        while (matcher.find()) {
            String field = matcher.group().split(":")[0];
            fieldsNumeric.add(field);
        }

        query = query.replace("Float:", "");
        query = query.replace("Numeric:", "");
        query = query.replaceAll("\"", "");
        query = query.replaceAll("\\(", "").replaceAll("\\)", "");

        String[] terms = query.split("OR |AND ");

        List<SearchResult> results = new ArrayList<>();

        long startTime, stopTime;
        List<HashMap<String, Object>> result = new ArrayList<>();

        HashMap<String, Object> extrafields = null;
        if (parameters.length > 0)
            extrafields = (HashMap<String, Object>) parameters[0];

        for (String term : terms) {
            result.addAll(this.databaseInterface.getCloserToMap(term, extrafields));
        }


        for (HashMap<String, Object> map : result) {
            SearchResult r = new SearchResult(URI.create((String) map.get("URI")), 1, map);
            results.add(r);
        }

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
