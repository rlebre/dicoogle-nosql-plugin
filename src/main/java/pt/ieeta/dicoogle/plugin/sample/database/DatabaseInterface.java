package pt.ieeta.dicoogle.plugin.sample.database;

import com.mongodb.Block;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Indexes;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import org.bson.Document;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseInterface {

    private DB db;
    private MongoClient mongo;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private String db_name;
    private GridFS gridFs;
    private DicomObjAux dicomObjAux;

    private Map<Integer, String> tagsDicom;

    /**
     * Construtor
     *
     * @param host
     * @param port
     * @param db_name
     * @param collectionName
     */
    public DatabaseInterface(String host, int port, String db_name, String collectionName) {
        this.mongo = new MongoClient(host, port);
        this.database = mongo.getDatabase(db_name);
        this.collection = database.getCollection(collectionName);
        this.db = this.mongo.getDB(db_name);
        this.db_name = db_name;
        this.gridFs = new GridFS(this.db);
        this.dicomObjAux = new DicomObjAux();

        this.tagsDicom = this.dicomObjAux.getAllDicomObjTags(); // Obter todas as tags do dicoogle
    }

    /**
     * (APAGAR ESTA FUNÇAO)
     *
     * @param dcmObj
     * @return
     */
    public boolean insertDicomObj(DicomObject dcmObj) {

        String filename = dcmObj.get(Tag.SOPInstanceUID).getValueAsString(dcmObj.getSpecificCharacterSet(), 0);

        ByteArrayOutputStream outStream;
        DicomOutputStream dcmOutStream;
        GridFSInputFile gridFsInputFile;

        try {
            outStream = new ByteArrayOutputStream();
            dcmOutStream = new DicomOutputStream(outStream);
            dcmOutStream.writeDicomFile(dcmObj);
            gridFsInputFile = this.gridFs.createFile(outStream.toByteArray());
            gridFsInputFile.setFilename(filename);
            gridFsInputFile.save(gridFsInputFile.getChunkSize());
            outStream.close();
        } catch (IOException ex) {
            Logger.getLogger(DatabaseInterface.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    /**
     * Insere os metadados de um DicomObject no MongoDB
     *
     * @param dcmObj
     */
    public void insertDicomObjJson(DicomObject dcmObj) {
        Map<String, String> dcmObjMap = this.dicomObjAux.getFieldsDicomObj(dcmObj);
        Document document = new Document();

        for (Map.Entry<String, String> entry : dcmObjMap.entrySet()) {
            document.append(entry.getKey(), entry.getValue());
        }

        this.collection.insertOne(document);
    }

    public void createIndexes() {
        // Criação de indices
        this.collection.createIndex(Indexes.ascending("PatientName"));
        this.collection.createIndex(Indexes.ascending("InstitutionName"));
        this.collection.createIndex(Indexes.ascending("Modality"));
        this.collection.createIndex(Indexes.ascending("StudyDate"));
    }

    public void executeQueriesTest() {
        long startTime, stopTime;

        // Para os testes finais NÃO IMPRIMIR!!!!

        System.out.println("Procurar os resultados de um paciente pelo nome - Deve obter resultados");
        startTime = System.currentTimeMillis();
        System.out.println(this.find("PatientName", "TOSHIBA^TARO").toString());
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

        System.out.println("Procurar os resultados de um paciente pelo nome -  Não deve obter resultados");
        startTime = System.currentTimeMillis();
        System.out.println(this.find("PatientName", "TOSHITARO").toString());
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

        System.out.println("Número de instituições distintas a realizar exames");
        startTime = System.currentTimeMillis();
        System.out.println(this.countDistinct("InstitutionName"));
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

        System.out.println("Número de exames por instituição");
        startTime = System.currentTimeMillis();
        System.out.println(this.countAggregator("InstitutionName"));
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

        System.out.println("Encontrar paciente com o nome mais próximo");
        startTime = System.currentTimeMillis();
        System.out.println(this.getCloserToMap("PatientName", "TOSHIBA^TAR"));
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

        List<String> fields = new ArrayList<>();
        fields.add("InstitutionName");
        fields.add("Modality");
        fields.add("PatientName");

        System.out.println("Agregar por instituição, modalidade, paciente");
        startTime = System.currentTimeMillis();
        System.out.println(this.CountMultAggregator(fields).toString());
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

    }

    public List<Document> find(String field, String value) {
        FindIterable<Document> docs = collection.find(eq(field, value));

        List<Document> results = new ArrayList();
        for (Document document : docs) {
            results.add(document);
        }

        return results;
    }

    public int countDistinct(String field) {
        // Contar, por exemplo, quantos tipos de exames diferentes existem
        int count = 0;

        AggregateIterable<Document> col = collection.aggregate(
                Arrays.asList(
                        Aggregates.group("$" + field, Accumulators.sum("count", 1))
                )
        );

        for (Document doc : col) {
            count++;
        }

        return count;
    }

    public Map<String, Integer> countAggregator(String field) {
        Map<String, Integer> map = new HashMap<String, Integer>();

        AggregateIterable<Document> col = collection.aggregate(
                Arrays.asList(
                        Aggregates.group("$" + field, Accumulators.sum("count", 1))
                )
        );

        for (Document doc : col) {
            String l = (String) doc.get("_id");
            Integer c = (Integer) doc.get("count");

            map.put(l, c);
        }

        return map;
    }

    public List<String> getCloserTo(String field, String value) {

        Document doc = new Document()
                .append("$regex", "(?)" + Pattern.quote(value))
                .append("$options", "i");

        Document match = new Document();
        match.append(field, doc);
        FindIterable<Document> iterable = collection.find(match);

        List<String> results = new ArrayList();
        for (Document document : iterable) {
            results.add((String) document.get(field));
        }

        return results;
    }

    public List<HashMap<String, Object>> getCloserToMap(String field, String value) {

        Document doc = new Document()
                .append("$regex", "(?)" + Pattern.quote(value))
                .append("$options", "i");

        Document match = new Document();
        match.append(field, doc);
        FindIterable<Document> iterable = collection.find(match);

        List<HashMap<String, Object>> results = new ArrayList<>();

        System.out.println("AQUI");

        /*or (Document document : iterable) {
            //results.add((String) document.get(field));
            System.out.println("Olá");
            map.put("SOPInstanceUID", document);
        }
        return map;*/

        for (Document document : iterable) {
            HashMap<String, Object> map = new HashMap<>();
            System.out.println("\n\n\n" + "Patient Name: " + ((String) document.get("PatientName")) + "\n\n\n");
            map.put("PatientID", (String) document.get("PatientID"));
            map.put("PatientName", (String) document.get("PatientName"));
            map.put("SOPInstanceUID", (String) document.get("SOPInstanceUID"));
            map.put("SeriesInstanceUID", (String) document.get("SeriesInstanceUID"));
            map.put("StudyInstanceUID", (String) document.get("StudyInstanceUID"));
            map.put("Modality", (String) document.get("Modality"));
            map.put("StudyDate", (String) document.get("StudyDate"));
            map.put("InstitutionName", (String) document.get("InstitutionName"));
            map.put("SeriesDate", (String) document.get("SeriesDate"));
            results.add(map);
        }
        return results;


    }

    public Map<Document, Integer> CountMultAggregator(List<String> fields) {
        Map<Document, Integer> loc = new HashMap<>();

        Document agg = new Document();
        for (String field : fields) {
            agg.append(field, "$" + field);
        }
        AggregateIterable<Document> col = collection.aggregate(
                Arrays.asList(
                        Aggregates.group(agg, Accumulators.sum("count", 1))
                )
        );

        for (Document doc : col) {
            Document l = (Document) doc.get("_id");
            Integer c = (Integer) doc.get("count");
            loc.put(l, c);
        }

        return loc;
    }

    Block<Document> printBlock = new Block<Document>() {
        public void apply(final Document document) {
            System.out.println(document.toJson());
        }
    };

}
