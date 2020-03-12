/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 * <p>
 * This file is part of Dicoogle/dicoogle-plugin-sample.
 * <p>
 * Dicoogle/dicoogle-plugin-sample is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Dicoogle/dicoogle-plugin-sample is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ieeta.dicoogle.plugin.sample.storage;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ieeta.dicoogle.plugin.sample.database.DatabaseInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

/**
 * Storage plugin for MongoDB.
 *
 * @author Rui Lebre - <ruilebre@ua.pt>
 */
public class SampleStoragePlugin implements StorageInterface {
    private static final Logger logger = LoggerFactory.getLogger(SampleStoragePlugin.class);

    private final Map<String, ByteArrayOutputStream> mem = new HashMap<>();
    private boolean enabled = true;
    private ConfigurationHolder settings;

    private DatabaseInterface databaseInterface = new DatabaseInterface("localhost", 27017, "DicoogleDatabase", "DicoogleObjs");

    public SampleStoragePlugin(DatabaseInterface databaseInterface) {
        this.databaseInterface = databaseInterface;
    }

    @Override
    public String getScheme() {
        return "mem://";
    }

    @Override
    public boolean handles(URI location) {
        return location.toString().contains("mem://");
    }


    @Override
    public Iterable<StorageInputStream> at(final URI location, Object... objects) {
        Iterable<StorageInputStream> c = new Iterable<StorageInputStream>() {

            @Override
            public Iterator<StorageInputStream> iterator() {
                Collection c2 = new ArrayList<>();
                StorageInputStream s = new StorageInputStream() {

                    @Override
                    public URI getURI() {
                        return location;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        ByteArrayOutputStream bos = mem.get(location.toString());
                        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
                        return bin;
                    }

                    @Override
                    public long getSize() throws IOException {
                        return mem.get(location.toString()).size();
                    }
                };
                c2.add(s);
                return c2.iterator();
            }
        };
        return c;
    }

    @Override
    public URI store(DicomObject dicomObject, Object... objects) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DicomOutputStream dos = new DicomOutputStream(bos);

        try {
            dos.writeDicomFile(dicomObject);
        } catch (IOException ex) {
            logger.warn("Failed to store object", ex);
        }
        bos.toByteArray();

        long startTime, stopTime;

        this.databaseInterface.createIndexes();

        //databaseInterface.insertDicomObj(dicomObject);
        // Insere o objeto na base de dados
        startTime = System.currentTimeMillis();
        this.databaseInterface.insertDicomObjJson(dicomObject);
        stopTime = System.currentTimeMillis();
        System.out.println("Insertation Time: " + (stopTime - startTime) + "ms.");

        this.databaseInterface.executeQueriesTest();

        //RESUME

        URI uri = URI.create("mem://" + UUID.randomUUID().toString());
        mem.put(uri.toString(), bos);

        return uri;
    }

    @Override
    public URI store(DicomInputStream inputStream, Object... objects) throws IOException {
        DicomObject obj = inputStream.readDicomObject();
        return store(obj);
    }

    @Override
    public void remove(URI location) {
        this.mem.remove(location.toString());
    }

    @Override
    public String getName() {
        return "sample-plugin-storage";
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
        // use settings here
    }

}
