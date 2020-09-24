package pt.ieeta.dicoogle.plugin.nosql;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseMiddleware;
import pt.ieeta.dicoogle.plugin.nosql.index.NoSqlIndexPlugin;
import pt.ieeta.dicoogle.plugin.nosql.query.NoSqlQueryPlugin;
import pt.ua.dicoogle.sdk.PluginBase;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.util.Collection;
import java.util.Collections;

/**
 * The main plugin set.
 * <p>
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
    private final NoSqlIndexPlugin json;

    private ConfigurationHolder settings;

    private String host;
    private int port;
    private String dbName;
    private String collectionName;
    private String dbUser;
    private String dbPassword;
    private DatabaseMiddleware databaseMiddleware;


    public NoSqlPluginSet() {
        logger.info("Initializing Distributed NoSql Plugin Set");

        NoSqlSettings defaultSettings = NoSqlSettings.getInstance();
        this.databaseMiddleware = new DatabaseMiddleware(defaultSettings.getHost(), defaultSettings.getPort(), defaultSettings.getDbName(), defaultSettings.getCollectionName(), defaultSettings.getDbUser(), defaultSettings.getDbPassword());
        this.query = new NoSqlQueryPlugin(databaseMiddleware);
        this.json = new NoSqlIndexPlugin(databaseMiddleware);

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
        this.dbUser = cnf.getString("dbUser", defaultSettings.getDbUser());
        this.dbPassword = cnf.getString("dbPassword", defaultSettings.getDbPassword());

        defaultSettings.setHost(this.host);
        defaultSettings.setPort(this.port);
        defaultSettings.setDbName(this.dbName);
        defaultSettings.setCollectionName(this.collectionName);
        defaultSettings.setDbUser(this.dbUser);
        defaultSettings.setDbPassword(this.dbUser);

        cnf.setProperty("host", this.host);
        cnf.setProperty("port", this.port);
        cnf.setProperty("dbName", this.dbName);
        cnf.setProperty("collectionName", this.collectionName);
        cnf.setProperty("dbUser", this.dbUser);
        cnf.setProperty("dbPassword", this.dbPassword);

        this.databaseMiddleware = new DatabaseMiddleware(defaultSettings.getHost(), defaultSettings.getPort(), defaultSettings.getDbName(), defaultSettings.getCollectionName(), defaultSettings.getDbUser(), defaultSettings.getDbPassword());
        this.query.setDatabaseInterface(this.databaseMiddleware);
        this.json.setDatabaseInterface(this.databaseMiddleware);

        try {
            cnf.save();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
}
