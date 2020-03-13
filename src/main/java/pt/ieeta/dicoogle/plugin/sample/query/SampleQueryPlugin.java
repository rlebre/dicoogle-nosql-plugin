package pt.ieeta.dicoogle.plugin.sample.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.sample.database.DatabaseInterface;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Example of a query provider.
 *
 * @author Rui Lebre - <ruilebre@ua.pt>
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
public class SampleQueryPlugin implements QueryInterface {
    private static final Logger logger = LoggerFactory.getLogger(SampleQueryPlugin.class);
    private boolean enabled;
    private ConfigurationHolder settings;

    private DatabaseInterface databaseInterface;

    public SampleQueryPlugin(DatabaseInterface databaseInterface) {
        this.enabled = true;
        this.databaseInterface = databaseInterface;
    }

    private SearchResult generateSearchResult() {

        HashMap<String, Object> map = new HashMap<>();
        map.put("PatientID", UUID.randomUUID().toString());
        map.put("PatientName", UUID.randomUUID().toString());
        map.put("SOPInstanceUID", UUID.randomUUID().toString());
        map.put("SeriesInstanceUID", UUID.randomUUID().toString());
        map.put("StudyInstanceUID", UUID.randomUUID().toString());
        map.put("Modality", "CT");
        map.put("StudyDate", "20150120");
        map.put("SeriesDate", "20150120");

        SearchResult r = new SearchResult(URI.create("file:" + File.separatorChar + UUID.randomUUID().toString()), 1, map);

        return r;
    }

    @Override
    public Iterable<SearchResult> query(String query, Object... parameters) {
        //this.databaseInterface =

        boolean advanced = query.charAt(0) != '(';
        System.out.println(advanced);
        String[] terms = query.split(" OR | AND ");
        String searchTerm = "";

        List<SearchResult> results = new ArrayList<>();

        long startTime, stopTime;
        List<HashMap<String, Object>> result = new ArrayList<>();

        if (!advanced) {
            for (int i = 0; i < terms.length; i++) {
                System.out.println(terms[i]);
                if (terms[i].startsWith("PatientName")) {
                    int pos = terms[i].indexOf(":");
                    int length = terms[i].length();
                    searchTerm = terms[i].substring(pos + 1);
                }
            }


            startTime = System.currentTimeMillis();
            result = this.databaseInterface.getCloserToMap("PatientName", searchTerm);
            result.addAll(this.databaseInterface.getCloserToMap("InstitutionName", searchTerm));
            result.addAll(this.databaseInterface.getCloserToMap("StudyDate", searchTerm));
            result.addAll(this.databaseInterface.getCloserToMap("Modality", searchTerm));
            stopTime = System.currentTimeMillis();
            System.out.println("Time: " + (stopTime - startTime) + "ms.");

        } else {
            for (String term : terms) {
                String[] splitTerm = term.split(":");
                result.addAll(this.databaseInterface.getCloserToMap(splitTerm[0], splitTerm[1]));
            }
        }

        for (HashMap<String, Object> map : result) {
            SearchResult r = new SearchResult(URI.create("file:" + File.separatorChar + UUID.randomUUID().toString()), 1, map);
            results.add(r);
        }


        return results;


    }

    @Override
    public String getName() {
        return "sample-plugin-query";
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
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
    }

}
