package pt.ieeta.dicoogle.plugin.nosql.index;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.nosql.database.DatabaseInterface;
import pt.ieeta.dicoogle.plugin.nosql.query.NoSqlQueryPlugin;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.datastructs.IndexReport2;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.ProgressCallable;
import pt.ua.dicoogle.sdk.task.Task;
import pt.ua.dicoogle.sdk.utils.TagValue;
import pt.ua.dicoogle.sdk.utils.TagsStruct;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;

import static pt.ieeta.dicoogle.plugin.nosql.index.IndexUtils.getValue;

/**
 * Indexer plugin for MongoDB
 *
 * @author Rui Lebre, ruilebre@ua.pt
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
public class NoSqlIndexPlugin implements IndexerInterface {
    private static final Logger logger = LoggerFactory.getLogger(NoSqlIndexPlugin.class);
    private DatabaseInterface databaseInterface;
    private NoSqlQueryPlugin queryPlugin;
    private boolean enabled;
    private ConfigurationHolder settings;

    public NoSqlIndexPlugin(DatabaseInterface databaseInterface) {
        this.enabled = true;
        this.databaseInterface = databaseInterface;
    }

    private synchronized void indexURI(StorageInputStream storage, IndexReport2 r) {
        Iterable<SearchResult> it = queryPlugin.query("uri:\"" + storage.getURI() + "\"");
        if (it == null) {
            logger.error("Could not find the query plugin");
            return;
        }

        if (it.iterator().hasNext()) {
            logger.info("File already exists, ignoring");
            return;
        }

        try (DicomInputStream dicomStream = new DicomInputStream(new BufferedInputStream(storage.getInputStream()))) {
            dicomStream.setHandler(new StopTagInputHandler(Tag.PixelData));
            DicomObject dicomObject = dicomStream.readDicomObject();

            long startTime, stopTime;

            String SOPInstanceUID = dicomObject.getString(Tag.SOPInstanceUID);
            it = queryPlugin.query("SOPInstanceUID:" + SOPInstanceUID);
            if (it == null) {
                logger.error("Could not find the query plugin");
                r.addError();
                return;
            }

            if (it.iterator().hasNext()) {
                logger.warn("SOPInstanceUID already exists: {}", SOPInstanceUID);
                r.addError();
                return;
            }

            startTime = System.currentTimeMillis();
            TagsStruct tagStruct = TagsStruct.getInstance();
            HashMap<String, String> tagValue = new HashMap<>();
            for (TagValue tag : tagStruct.getDIMFields()) {
                DicomElement e = dicomObject.get(tag.getTagNumber());
                String value = "";
                if (e != null) {
                    value = getValue(e);
                }
                tagValue.put(tag.getName(), value);
            }

            if (tagStruct.isModalityEnable(dicomObject.getString(Tag.Modality)) || tagStruct.isIndexAllModalitiesEnabled()) {
                // TODO: Currently it is only indexing the DIM required. Handling all DICOM TAGS is necessary
            }

            tagValue.put("URI", storage.getURI().toString());

            this.databaseInterface.insertDicomObjMap(tagValue);
            stopTime = System.currentTimeMillis();

            logger.info("Insertion Time: ", (stopTime - startTime), "ms.");
            r.addIndexFile();
        } catch (IOException e) {
            logger.warn("Failed to index \"{}\"", storage.getURI(), e);
            r.addError();
        }
    }

    @Override
    public Task<Report> index(final StorageInputStream file, Object... objects) {
        Task task = new Task<>(
                new ProgressCallable<Report>() {
                    private float progress = 0.0f;

                    @Override
                    public Report call() {
                        logger.debug("Started single index task: {}", file.getURI());

                        IndexReport2 r = new IndexReport2();
                        r.started();

                        if (!handles(file.getURI())) {
                            r.addError();
                            logger.error("Indexation of \"{}\" failed.", file.getURI());
                        } else {
                            indexURI(file, r);
                            logger.info("Finished single index task: {},{}", this.hashCode(), r);
                        }

                        progress = 1.0f;
                        r.finished();
                        return r;
                    }

                    @Override
                    public float getProgress() {
                        return progress;
                    }
                });

        this.databaseInterface.createIndexes();
        return task;
    }

    @Override
    public Task<Report> index(final Iterable<StorageInputStream> files, Object... objects) {
        Task task = new Task<>(
                new ProgressCallable<Report>() {
                    private float progress = 0.0f;

                    @Override
                    public Report call() {
                        logger.debug("Started Index Task: {}", this.hashCode());
                        IndexReport2 r = new IndexReport2();
                        r.started();

                        Iterator<StorageInputStream> it = files.iterator();
                        int i = 1;

                        while (it.hasNext()) {
                            StorageInputStream s = it.next();
                            if (!handles(s.getURI())) {
                                continue;
                            }

                            logger.debug("Started indexing: {},{},{}", this.hashCode(), i, s.getURI());
                            try {
                                indexURI(s, r);
                            } catch (Exception e) {
                                logger.error("ERROR indexing: {},{},{}", this.hashCode(), i, s.getURI(), e);
                                r.addError();
                            }

                            logger.info("Finished indexing: {},{},{},{}", this.hashCode(), i, s.getURI(), r);
                            i++;
                        }

                        progress = 1.0f;
                        r.finished();
                        logger.info("Finished Index Task: {},{}", this.hashCode(), r);
                        return r;
                    }

                    @Override
                    public float getProgress() {
                        return progress;
                    }
                });

        this.databaseInterface.createIndexes();
        return task;
    }

    @Override
    public boolean unindex(URI uri) {
        long documentsDeleted = databaseInterface.removeEntriesBasedOn("URI", uri.toString());
        logger.info("Unindexed {} documents successfully.", documentsDeleted);
        return documentsDeleted >= 0;
    }

    /**
     * This method is used to retrieve the unique name of the indexer.
     *
     * @return a fixed name for the indexer
     */
    @Override
    public String getName() {
        return "dicoogle-nosql-indexer";
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

    @Override
    public boolean handles(URI path) {
        int indexExt = path.toString().lastIndexOf('.');
        if (indexExt == -1) {
            return true; // handles files with no extensions
        }

        String extension = path.toString().substring(indexExt);
        switch (extension.toLowerCase()) {
            case ".dicom":
            case ".dcm":
                return true;  // handles files with dcm extensions
            default:
                return false; // everything else, it does not handle
        }
    }

    public void setDatabaseInterface(DatabaseInterface databaseInterface) {
        this.databaseInterface = databaseInterface;
        this.queryPlugin = new NoSqlQueryPlugin(this.databaseInterface);
    }
}
