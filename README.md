# Dicoogle Distributed NoSQL plugin - MongoDB

Dicoogle plugin for distributed database of DICOM metadata using MongoDB Sharding.
The plugin indexes all metadata as it is configured in Dicoogle (by default, it indexes the DIM
required fields) except the PixelData.

## Requirements

- Running:
  - Dicoogle v2.5.0
  - MongoDB
- Testing:
  - dcmtk

## Instructions

1. Compile and build: `mvn install`;

2. Copy the build `target/distributed-nosql-2.5.0-jar-with-dependencies.jar`
   to your `DicoogleDir/Plugins` folder;

3. Run Dicoogle. Example: `sh DicoogleServer.sh`.

### Configuration

Plugin configuration is available at `DicoogleDir/Plugins/settings/dicoogle-distributed-nosql.xml`
Upon initialization, if no configurations file is supplied, the Dicoogle Platform
creates one with the default values. This plugin allows the configuration of the MongoDB host,
port, database name and database collection. Example:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<configuration>
    <host>localhost</host>
    <port>27017</port>
    <dbName>DicoogleDatabase</dbName>
    <collectionName>DicoogleObjs</collectionName>
</configuration>
```

### Test the query plugin

1. Activate the _Query Retrieve Service_ in Dicoogle Management tab available at
   `localhost:8080/#/management` running on port 1045

2. Use one of the following services:

   - HTTP search endpoint:

   ```http request
   http://localhost:8080/searchDIM?query=PatientID:*&keyword=true
   ```

   - DCMTK query toolkit:

   ```shell script
   findscu -S -k 0008,0052="IMAGE" -k PatientID="*" -aec DICOOGLE-STORAGE localhost 1045
   ```

### Test the index plugin

1.  Activate the _Storage Service_ in Dicoogle Management tab available at
    `localhost:8080/#/management` running on port 6666

2.  Navigate to the folder where the DICOM dataset is contained. Example:
    `cd ~/dicom-dataset`
3.  Run the following command to recursively find every file ending with `.dcm`
    and using `dcmtk`toolkit, sending via C-STORE to Dicoogle.
    ```shell script
    dcmsend -aec DICOOGLE-STORAGE localhost 6666 $(find . -name '*.dcm' -print)
    ```
    Alternatively, you can skip the step 1. and run the following command, replacing
    \<dir\> with the directory path to your DICOM dataset folder.
    ```shell script
    dcmsend -aec DICOOGLE-STORAGE localhost 6666 $(find <dir> -name '*.dcm' -print) 
    ```

## TODO List

- [x] Support complex queries (with AND and OR)
- [ ] Support to index all the DICOM TAGS; currently only the DIM are supported
- [ ] Use _sync_ and _async_ MongoDB drivers for different cases; for instance,
      use _async_ for insert and delete MongoDB documents and use _sync_ for find/query
      operations where the user should wait for the data retrieval

## Acknowledgments

- Thank you to the contributors of [dicoogle-mongo-plugin](bioinformatics-ua/dicoogle-mongo-plugin)
  as the translation of queries to MongoDB queries was adapted from their work
