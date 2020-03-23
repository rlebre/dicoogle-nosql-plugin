package pt.ieeta.dicoogle.plugin.nosql.index;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.ProgressCallable;
import pt.ua.dicoogle.sdk.task.Task;

import java.io.BufferedInputStream;
import java.net.URI;

/**
 * Example of an indexer plugin.
 *
 * @author Rui Lebre - <ruilebre@ua.pt>
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
public class NoSqlJsonPlugin implements IndexerInterface {
    private static final Logger logger = LoggerFactory.getLogger(NoSqlJsonPlugin.class);
    private boolean enabled;
    private ConfigurationHolder settings;

    public NoSqlJsonPlugin() {
        this.enabled = true;
    }

    private Report indexURI(StorageInputStream storage) {

        try (DicomInputStream dicomStream = new DicomInputStream(new BufferedInputStream(storage.getInputStream()))) {

            dicomStream.setHandler(new StopTagInputHandler(Tag.PixelData));
            DicomObject dicomObject = dicomStream.readDicomObject();

            String PatientName = dicomObject.getString(Tag.PatientName);
            String StudyInstanceUID = dicomObject.getString(Tag.StudyInstanceUID);
            String SeriesInstanceUID = dicomObject.getString(Tag.SeriesInstanceUID);
            String SOPInstanceUID = dicomObject.getString(Tag.SOPInstanceUID);

            // use slf4j for logging purposes:
            logger.info("SOP Instance UID: {}", SOPInstanceUID);
            logger.info("PatientName: {}", PatientName);
        } catch (Exception e) {
            logger.warn("Failed to index \"{}\"", storage.getURI(), e);
            System.err.println("indexURI: Do whatever you want.");
        }

        return new Report();
    }

    @Override
    public Task<Report> index(final StorageInputStream file, Object... objects) {


        return new Task<>(
                new ProgressCallable<Report>() {
                    private float progress = 0.0f;

                    @Override
                    public Report call() {

                        Report r = null;
                        try {
                            r = indexURI(file);
                        } catch (Exception e) {
                            logger.warn("Indexation of \"{}\" failed", file.getURI(), e);
                        }

                        progress = 1.0f;

                        return r;
                    }

                    @Override
                    public float getProgress() {
                        return progress;
                    }
                });

    }

    @Override
    public Task<Report> index(final Iterable<StorageInputStream> files, Object... objects) {
        return new Task<>(
                new ProgressCallable<Report>() {
                    private float progress = 0.0f;

                    @Override
                    public Report call() {

                        Report r = new Report();
                        try {
                            for (StorageInputStream f : files) {
                                r = indexURI(f);
                            }
                        } catch (Exception e) {
                            logger.warn("Indexation failed", e);
                        }

                        progress = 1.0f;

                        return r;
                    }

                    @Override
                    public float getProgress() {
                        return progress;
                    }
                });

    }

    @Override
    public boolean unindex(URI uri) {
        // Not implemented
        return false;
    }

    /**
     * This method is used to retrieve the unique name of the indexer.
     *
     * @return a fixed name for the indexer
     */
    @Override
    public String getName() {
        return "dicoogle-nosql-json";
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

    @Override
    public boolean handles(URI path) {
        // State here whether this indexer can index the file at the given path.
        // If not sure, simply return true and let the indexation procedures find out.
        return true;
    }

}
