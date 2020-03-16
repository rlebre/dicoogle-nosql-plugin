package pt.ieeta.dicoogle.plugin.nosql;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseInterface;
import pt.ieeta.dicoogle.plugin.nosql.index.NoSqlJsonPlugin;
import pt.ieeta.dicoogle.plugin.nosql.query.NoSqlQueryPlugin;
import pt.ieeta.dicoogle.plugin.nosql.storage.NoSqlStoragePlugin;
import pt.ua.dicoogle.sdk.*;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.util.*;

/**
 * The main plugin set.
 *
 * This is the entry point for all plugins.
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

    private DatabaseInterface databaseInterface = new DatabaseInterface("localhost", 27017, "DicoogleDatabase", "DicoogleObjs");


    public NoSqlPluginSet() {
        logger.info("Initializing NoSql Plugin Set");

        this.query = new NoSqlQueryPlugin(databaseInterface);
        this.storage = new NoSqlStoragePlugin(databaseInterface);
        this.json = new NoSqlJsonPlugin();

        logger.info("NoSql Plugin Set is ready");
    }

    @Override
    public Collection<IndexerInterface> getIndexPlugins() {
        List<IndexerInterface> c = new LinkedList<IndexerInterface>();
        c.add(this.json);
        return c;
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
    }

    @Override
    public Collection<GraphicalInterface> getGraphicalPlugins() {
        // Graphical plugins are deprecated. Do not use or provide any.
        return Collections.EMPTY_LIST;
    }
}
