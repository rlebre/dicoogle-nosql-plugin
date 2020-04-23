package pt.ieeta.dicoogle.plugin.nosql.database;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.gridfs.GridFS;
import org.bson.Document;
import org.dcm4che2.data.DicomObject;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

/**
 * Database interface to communicate with MongoDB
 *
 * @author Rui Lebre, ruilebre@ua.pt
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
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
     * @param dbName
     * @param collectionName
     */
    public DatabaseInterface(String host, int port, String dbName, String collectionName) {
        this.mongo = new MongoClient(host, port);
        this.database = mongo.getDatabase(dbName);
        this.collection = database.getCollection(collectionName);
        this.db = this.mongo.getDB(dbName);
        this.db_name = dbName;
        this.gridFs = new GridFS(this.db);
        this.dicomObjAux = new DicomObjAux();

        this.tagsDicom = this.dicomObjAux.getAllDicomObjTags(); // Obter todas as tags do dicoogle
    }

    /**
     * Insere os metadados de um DicomObject no MongoDB
     *
     * @param dcmObj
     */
    public void insertDicomObjJson(DicomObject dcmObj, URI uri) {
        Map<String, String> dcmObjMap = this.dicomObjAux.getFieldsDicomObj(dcmObj);
        Document document = new Document();

        for (Map.Entry<String, String> entry : dcmObjMap.entrySet()) {
            document.append(entry.getKey(), entry.getValue());
        }

        document.append("URI", uri.toString());
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
        System.out.println(this.getCloserToMap("PatientName:TOSHIBA^TAR", new HashMap<String, Object>()));
        stopTime = System.currentTimeMillis();
        System.out.println("Time: " + (stopTime - startTime) + "ms.");

        List<String> fields = new ArrayList<>();
        fields.add("InstitutionName");
        fields.add("Modality");
        fields.add("PatientName");

        System.out.println("Agregar por instituição, modalidade, paciente");
        startTime = System.currentTimeMillis();
        System.out.println(this.countMultAggregator(fields).toString());
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
        Map<String, Integer> map = new HashMap<>();

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

    public List<HashMap<String, Object>> getCloserToMap(String terms, HashMap<String, Object> extrafields) {
        String field = terms.split(":")[0];
        String value = terms.split(":")[1];
        Document doc = new Document()
                .append("$regex", "(?)" + Pattern.quote(value))
                .append("$options", "i");

        Document match = new Document();
        match.append(field, doc);
        FindIterable<Document> iterable = collection.find(match);

        List<HashMap<String, Object>> results = new ArrayList<>();

        System.out.println("AQUI");


        for (Document document : iterable) {
            Iterator it = extrafields.entrySet().iterator();
            HashMap<String, Object> map = new HashMap<>();
            while (it.hasNext()) {
                Map.Entry<String, Object> pair = (Map.Entry) it.next();
                map.put(pair.getKey(), document.get(pair.getValue()));
                it.remove();
                results.add(map);
            }
            map.put("URI", document.get("URI"));
        }

        return results;
    }

    public Map<Document, Integer> countMultAggregator(List<String> fields) {
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

    public long removeEntriesBasedOn(String key, String value) {
        DeleteResult result = collection.deleteMany(eq(key, value));
        return result.getDeletedCount();
    }
}
