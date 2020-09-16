package pt.ieeta.dicoogle.plugin.nosql.database;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example of an indexer plugin.
 *
 * @author Rui Lebre, ruilebre@ua.pt
 * @author Ana Almeida
 * @author Francisco Oliveira
 */
public class DicomObjAux {

    private Map<Integer, String> tagsDicom;
    private Field[] tags;


    public static DicomObjAux getInstance() {
        return instance;
    }

    private static final DicomObjAux instance = new DicomObjAux();

    private DicomObjAux() {
        this.tagsDicom = new HashMap<>();
        this.tags = Tag.class.getFields();
    }

    /**
     * Obtém todas as tags possíveis que um DicomObject pode ter
     */
    public Map<Integer, String> getAllDicomObjTags() {

        for (int i = 0; i < tags.length; i++) {
            try {
                this.tagsDicom.put(tags[i].getInt(null), tags[i].getName());
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DatabaseMiddleware.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DatabaseMiddleware.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return this.tagsDicom;
    }

    /**
     * Extrai os metadados do DicomObject para um mapa
     *
     * @param dcmObj
     * @return
     */
    public Map<String, String> getFieldsDicomObj(DicomObject dcmObj) {
        Map<String, String> dcmObjMap = new HashMap<>();

        for (Map.Entry<Integer, String> entry : this.tagsDicom.entrySet()) {
            Integer tag = entry.getKey();
            String tagName = entry.getValue();

            try {
                if (dcmObj.getString(tag) != null) {
                    dcmObjMap.put(tagName, dcmObj.getString(tag));
                }
            } catch (UnsupportedOperationException ex) {
                System.out.println("[ERROR ON] TAG: " + tagName);
                Logger.getLogger(DatabaseMiddleware.class.getName()).log(Level.SEVERE, null, ex);
            }


        }

        return dcmObjMap;
    }

    public String tagName(int tag) {
        return this.tagsDicom.get(tag);
    }
}
