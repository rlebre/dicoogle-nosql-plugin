package pt.ieeta.dicoogle.plugin.nosql;

/**
 * Default settings file
 *
 * @author Rui Lebre, ruilebre@ua.pt
 */
public class NoSqlSettings {
    private static NoSqlSettings instance = null;
    private String host = "localhost";
    private int port = 27017;
    private String dbName = "DicoogleDatabase";
    private String collectionName = "DicoogleObjs";

    public static NoSqlSettings getInstance() {
        if (instance == null)
            instance = new NoSqlSettings();
        return instance;

    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
