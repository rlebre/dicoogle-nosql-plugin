package pt.ieeta.dicoogle.plugin.nosql.database;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import pt.ua.dicoogle.sdk.utils.TagValue;
import pt.ua.dicoogle.sdk.utils.TagsStruct;

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
    private MongoClient mongo;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    /**
     * Constructor
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
    }

    /**
     * Inserts a HashMap containing TAG:VALUE to a MongoDB document
     *
     * @param dicomMap HashMap to insert
     */
    public void insertDicomObjMap(HashMap<String, String> dicomMap) {
        Document document = new Document();

        for (Map.Entry<String, String> entry : dicomMap.entrySet()) {
            document.append(entry.getKey(), entry.getValue());
        }

        this.collection.insertOne(document);
    }

    public void createIndexes() {
        TagsStruct tagStruct = TagsStruct.getInstance();
        Set<TagValue> dimTags = tagStruct.getDIMFields();

        for (TagValue tag : dimTags) {
            this.collection.createIndex(Indexes.ascending(tag.getName()));
        }
    }

    public List<Document> find(String field, String value) {
        FindIterable<Document> docs = collection.find(eq(field, value));

        List<Document> results = new ArrayList();
        for (Document document : docs) {
            results.add(document);
        }

        return results;
    }

    public List<HashMap<String, Object>> find(String tag, String value, Map<String, Object> extrafields) {
        Document doc = new Document()
                .append("$regex", "(?)" + Pattern.quote(value))
                .append("$options", "i");

        Document match = new Document();
        match.append(tag, doc);
        FindIterable<Document> iterable = collection.find(match);

        List<HashMap<String, Object>> results = new ArrayList<>();

        for (Document document : iterable) {
            document.remove("_id");

            Iterator it = extrafields.entrySet().iterator();
            HashMap<String, Object> map = new HashMap<>();

            if (it.hasNext()) {
                map.put("URI", document.get("URI"));
            }

            while (it.hasNext()) {
                Map.Entry<String, Object> pair = (Map.Entry) it.next();
                map.put(pair.getKey(), document.get(pair.getValue()));
                it.remove();
            }

            results.add(map);
        }

        return results;
    }

    /**
     * Removes all entries in the MongoDB database matching the key and value
     *
     * @param key
     * @param value
     * @return How many records were deleted
     */
    public long removeEntriesBasedOn(String key, String value) {
        DeleteResult result = collection.deleteMany(eq(key, value));
        return result.getDeletedCount();
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
        System.out.println(this.find("PatientName", "TOSHIBA^TAR", new HashMap<String, Object>()));
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

    public int countDistinct(String field) {
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
}
