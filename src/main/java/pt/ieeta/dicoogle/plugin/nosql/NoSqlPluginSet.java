package pt.ieeta.dicoogle.plugin.nosql;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.XMLConfiguration;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseInterface;
import pt.ieeta.dicoogle.plugin.nosql.index.NoSqlJsonPlugin;
import pt.ieeta.dicoogle.plugin.nosql.query.NoSqlQueryPlugin;
import pt.ieeta.dicoogle.plugin.nosql.storage.NoSqlStoragePlugin;
import pt.ua.dicoogle.sdk.*;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * The main plugin set.
 * <p>
 * This is the entry point for the storage, index and query distributed NoSQL plugins
 *
 * @author Rui Lebre - <ruilebre@ua.pt>
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
@PluginImplementation
public class NoSqlPluginSet implements PluginSet {
    private static final Logger logger = LoggerFactory.getLogger(NoSqlPluginSet.class);

    private final NoSqlQueryPlugin query;
    private final NoSqlStoragePlugin storage;
    private final NoSqlJsonPlugin json;

    private ConfigurationHolder settings;

    private DatabaseInterface databaseInterface;


    public NoSqlPluginSet() {
        logger.info("Initializing NoSql Plugin Set");

        this.databaseInterface = new DatabaseInterface("localhost", 27017, "DicoogleDatabase", "DicoogleObjs");
        this.query = new NoSqlQueryPlugin(databaseInterface);
        this.storage = new NoSqlStoragePlugin(databaseInterface);
        this.json = new NoSqlJsonPlugin();

        logger.info("NoSql Plugin Set is ready");
    }

    @Override
    public Collection<IndexerInterface> getIndexPlugins() {
        return Arrays.asList(this.json);
    }

    @Override
    public Collection<QueryInterface> getQueryPlugins() {
        return Arrays.asList(this.query);
    }

    @Override
    public String getName() {
        return "NoSql Plugin Set";
    }

    @Override
    public Collection<ServerResource> getRestPlugins() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<JettyPluginInterface> getJettyPlugins() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Collection<StorageInterface> getStoragePlugins() {
        return Arrays.asList(this.storage);
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
        XMLConfiguration cnf = xmlSettings.getConfiguration();

        cnf.setThrowExceptionOnMissing(true);

        String host = cnf.getString("host", "localhost");
        int port = cnf.getInt("port", 27017);
        String dbName = cnf.getString("dbName", "DicoogleDatabase");
        String collectionName = cnf.getString("collectionName", "DicoogleObjs");

        cnf.setProperty("host", host);
        cnf.setProperty("port", port);
        cnf.setProperty("dbName", dbName);
        cnf.setProperty("collectionName", collectionName);
    }

    @Override
    public Collection<GraphicalInterface> getGraphicalPlugins() {
        // Graphical plugins are deprecated. Do not use or provide any.
        return Collections.EMPTY_LIST;
    }
}
