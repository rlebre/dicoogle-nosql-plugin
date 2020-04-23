package pt.ieeta.dicoogle.plugin.nosql;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseInterface;
import pt.ieeta.dicoogle.plugin.nosql.index.NoSqlJsonPlugin;
import pt.ieeta.dicoogle.plugin.nosql.query.NoSqlQueryPlugin;
import pt.ua.dicoogle.sdk.PluginBase;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.util.Collection;
import java.util.Collections;

/**
 * The main plugin set.
 *
 * This is the entry point for the storage, index and query distributed NoSQL plugins
 *
 * @author Rui Lebre, ruilebre@ua.pt
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
@PluginImplementation
public class NoSqlPluginSet extends PluginBase {
    private static final Logger logger = LoggerFactory.getLogger(NoSqlPluginSet.class);

    private final NoSqlQueryPlugin query;
    private final NoSqlJsonPlugin json;

    private ConfigurationHolder settings;

    private String host;
    private int port;
    private String dbName;
    private String collectionName;
    private DatabaseInterface databaseInterface;


    public NoSqlPluginSet() {
        logger.info("Initializing Distributed NoSql Plugin Set");

        NoSqlSettings defaultSettings = NoSqlSettings.getInstance();
        this.databaseInterface = new DatabaseInterface(defaultSettings.getHost(), defaultSettings.getPort(), defaultSettings.getDbName(), defaultSettings.getCollectionName());
        this.query = new NoSqlQueryPlugin(databaseInterface);
        this.json = new NoSqlJsonPlugin(databaseInterface);

        this.queryPlugins.add(this.query);
        this.indexPlugins.add(this.json);

        logger.info("Distributed NoSql Plugin Set is ready");
    }

    @Override
    public String getName() {
        return "dicoogle-distributed-nosql";
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Collection<StorageInterface> getStoragePlugins() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
        XMLConfiguration cnf = this.settings.getConfiguration();
        cnf.setThrowExceptionOnMissing(true);

        NoSqlSettings defaultSettings = NoSqlSettings.getInstance();
        this.host = cnf.getString("host", defaultSettings.getHost());
        this.port = cnf.getInt("port", defaultSettings.getPort());
        this.dbName = cnf.getString("dbName", defaultSettings.getDbName());
        this.collectionName = cnf.getString("collectionName", defaultSettings.getCollectionName());

        defaultSettings.setHost(this.host);
        defaultSettings.setPort(this.port);
        defaultSettings.setDbName(this.dbName);
        defaultSettings.setCollectionName(this.collectionName);

        cnf.setProperty("host", this.host);
        cnf.setProperty("port", this.port);
        cnf.setProperty("dbName", this.dbName);
        cnf.setProperty("collectionName", this.collectionName);

        this.databaseInterface = new DatabaseInterface(defaultSettings.getHost(), defaultSettings.getPort(), defaultSettings.getDbName(), defaultSettings.getCollectionName());
        this.query.setDatabaseInterface(this.databaseInterface);
        this.json.setDatabaseInterface(this.databaseInterface);

        try {
            cnf.save();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
}
