package pt.ieeta.dicoogle.plugin.sample;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.sample.database.DatabaseInterface;
import pt.ieeta.dicoogle.plugin.sample.index.SampleJsonPlugin;
import pt.ieeta.dicoogle.plugin.sample.query.SampleQueryPlugin;
import pt.ieeta.dicoogle.plugin.sample.storage.SampleStoragePlugin;
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
public class SamplePluginSet implements PluginSet {
    private static final Logger logger = LoggerFactory.getLogger(SamplePluginSet.class);

    private final SampleQueryPlugin query;
    private final SampleStoragePlugin storage;
    private final SampleJsonPlugin json;

    private ConfigurationHolder settings;

    private DatabaseInterface databaseInterface = new DatabaseInterface("localhost", 27017, "DicoogleDatabase", "DicoogleObjs");


    public SamplePluginSet() {
        logger.info("Initializing Sample Plugin Set");

        this.query = new SampleQueryPlugin(databaseInterface);
        this.storage = new SampleStoragePlugin(databaseInterface);
        this.json = new SampleJsonPlugin();

        logger.info("Sample Plugin Set is ready");
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
        return "Sample Plugin Set";
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
