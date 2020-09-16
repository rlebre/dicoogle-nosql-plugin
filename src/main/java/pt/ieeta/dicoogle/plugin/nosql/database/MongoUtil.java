package pt.ieeta.dicoogle.plugin.nosql.database;

import org.bson.Document;
import org.dcm4che2.data.Tag;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Rui Lebre, ruilebre@ua.pt
 * @author Louis
 */
public class MongoUtil {

    public static List<SearchResult> getListFromResult(List<Document> dbObjs, URI location, float score) {
        ArrayList<SearchResult> result = new ArrayList<SearchResult>();
        String strSOPUID = DicomObjAux.getInstance().tagName(Tag.SOPInstanceUID);
        for (int i = 0; i < dbObjs.size(); i++) {
            SearchResult searchResult;
            if (dbObjs.get(i).get(strSOPUID) != null) {
                String str = location.toString() + dbObjs.get(i).get(strSOPUID);
                URI uri = null;
                try {
                    uri = new URI(str);
                } catch (URISyntaxException e) {
                    uri = location;
                }
                HashMap<String, Object> map = new HashMap<String, Object>();
                HashMap<String, Object> mapTemp = (HashMap<String, Object>) dbObjs.get(i).values();
                for (String mapKey : mapTemp.keySet()) {
                    if (mapTemp.get(mapKey) == null) {
                        map.put(mapKey, mapTemp.get(mapKey));
                    } else {
                        map.put(mapKey, mapTemp.get(mapKey).toString());
                    }
                }
                searchResult = new SearchResult(uri, score, map);
                result.add(searchResult);
            }
        }
        return result;
    }

    private static Document decodeStringToQuery(String strQuery) {
        Document query;
        Object obj, lowObj, highObj;
        String str = "", field = "", lowValue = "", highValue = "";
        int length = strQuery.length(), cmp = 0;
        char currentChar;
        boolean isField = true, isInclusiveBetween = false, isExclusiveBetween = false, isNot = false;
        while (cmp < length) {
            currentChar = strQuery.charAt(cmp);
            cmp++;
            switch (currentChar) {
                case ':':
                    if (isField) {
                        isField = false;
                        field = str;
                        str = "";
                    }
                    if (str.equalsIgnoreCase("Numeric")) {
                        str = "";
                    }
                    break;
                case '[':
                    isInclusiveBetween = true;
                    str = "";
                    break;
                case ']':
                    highValue = str;
                    break;
                case '{':
                    isExclusiveBetween = true;
                    str = "";
                    break;
                case '}':
                    highValue = str;
                    break;
                case ' ':
                    if (str.equalsIgnoreCase("NOT")) {
                        isNot = true;
                        str = "";
                        break;
                    }
                    String temp = strQuery.substring(cmp, cmp + 2);
                    if (temp.equalsIgnoreCase("TO")) {
                        lowValue = str;
                        str = "";
                        cmp += 2;
                    }
                    break;
                default:
                    str += currentChar;
                    break;
            }
        }
        if (isInclusiveBetween || isExclusiveBetween) {
            try {
                lowObj = Double.parseDouble(lowValue);
            } catch (NumberFormatException e) {
                lowObj = lowValue;
            }
            try {
                highObj = Double.parseDouble(highValue);
            } catch (NumberFormatException e) {
                highObj = highValue;
            }
            query = madeQueryIsBetween(field, lowObj, highObj, isInclusiveBetween);
            return query;
        }
        /*try {
            obj = Double.parseDouble(str);
            query = madeQueryIsValue(field, obj, isNot);
        } catch (NumberFormatException e) {
            obj = str;
            query = madeQueryIsValueRegexInsensitive(field, obj, isNot);
        }*/
        query = madeQueryIsValueRegexInsensitive(field, str, isNot);

        return query;
    }

    public static Document parseStringToQuery(String strQuery) {
        Document query = null;
        String str = "";
        char currentChar;
        int cmp = 0, length, nbParOpen = 0, nbBrackets = 0;
        boolean and = false, or = false, isBlank = true;
        if (strQuery == null || strQuery.equalsIgnoreCase("")) {
            return madeQueryFindAll();
        }
        length = strQuery.length();
        for (int i = 0; i < length; i++) {
            currentChar = strQuery.charAt(i);
            if (currentChar != ' ' && currentChar != '*' && currentChar != '"' && currentChar != ':') {
                isBlank = false;
            }
        }
        if (isBlank) {
            return madeQueryFindAll();
        }
        while (cmp != length) {
            currentChar = strQuery.charAt(cmp);
            cmp++;
            switch (currentChar) {
                case '{':
                case '[':
                    str += currentChar;
                    nbBrackets++;
                    break;
                case '}':
                case ']':
                    str += currentChar;
                    nbBrackets--;
                    break;
                case '(':
                    if (nbParOpen != 0) {
                        str += currentChar;
                    }
                    nbParOpen++;
                    break;
                case ')':
                    nbParOpen--;
                    if (nbParOpen == 0) {
                        if (!and && !or) {
                            query = parseStringToQuery(str);
                        }
                        if (and) {
                            query = madeQueryAND(query, parseStringToQuery(str));
                            and = false;
                        }
                        if (or) {
                            query = madeQueryOR(query, parseStringToQuery(str));
                            or = false;
                        }
                        str = "";
                    } else {
                        str += currentChar;
                    }
                    break;
                case ' ':
                    if (str.equalsIgnoreCase("NOT")) {
                        str += currentChar;
                        break;
                    }
                    if (nbBrackets != 0) {
                        str += currentChar;
                        break;
                    }
                    if (nbParOpen != 0) {
                        str += currentChar;
                        break;
                    }
                    if (str.equalsIgnoreCase("AND") || str.equalsIgnoreCase("OR")) {
                        if (str.equalsIgnoreCase("AND")) {
                            and = true;
                        } else {
                            or = true;
                        }
                        str = "";
                    } else {
                        String temp = "";
                        if (cmp + 3 < length) {
                            temp = strQuery.substring(cmp, cmp + 3);
                        }
                        if (temp.equalsIgnoreCase("AND") || temp.equalsIgnoreCase("OR ")) {
                            if (!and && !or) {
                                if (!str.equals("")) {
                                    query = decodeStringToQuery(str);
                                }
                            }
                            if (and) {
                                query = madeQueryAND(query, decodeStringToQuery(str));
                                and = false;
                            }
                            if (or) {
                                query = madeQueryOR(query, decodeStringToQuery(str));
                                or = false;
                            }
                            str = "";
                        }
                    }
                    break;
                default:
                    str += currentChar;
                    break;
            }
        }
        if (!str.equals("")) {
            if (!and && !or) {
                query = decodeStringToQuery(str);
            }
            if (and) {
                query = madeQueryAND(query, decodeStringToQuery(str));
            }
            if (or) {
                query = madeQueryOR(query, decodeStringToQuery(str));
            }
        }
        return query;
    }

    private static Document madeQueryFindAll() {
        Document query = new Document();
        return query;
    }

    private static Document madeQueryIsValue(String field, Object value, boolean isNot) {
        Document query = new Document();
        String str = field;
        if (!isNot) {
            query.put(str, value);
        } else {
            query.put(str, new Document("$ne", value));
        }
        return query;
    }

    private static Document madeQueryIsValueRegexInsensitive(String field, Object value, boolean isNot) {
        Document query = new Document();
        String str = field;
        String strValue = (String) value;
        if (strValue.endsWith(".*")) {
            strValue = strValue.substring(0, strValue.length() - 1);
        } else if (strValue.endsWith("*")) {
            strValue = strValue.substring(0, strValue.length() - 1);
        }
        if (!isNot) {
            query.put(str, new Document("$regex", "^" + strValue + ".*").append("$options", "i"));
        } else {
            query.put(str, new Document("$ne", strValue));
        }
        return query;
    }

    private static Document madeQueryIsBetween(String field, Object lowValue, Object highValue, boolean isInclusive) {
        Document query = new Document();
        String str = field;
        if (!isInclusive) {
            query.put(str, new Document("$gt", lowValue).append("$lt", highValue));
        } else {
            query.put(str, new Document("$gte", lowValue).append("$lte", highValue));
        }
        return query;
    }

    private static Document madeQueryAND(Document dbObj1, Document dbObj2) {
        return madeQueryGeneral(dbObj1, dbObj2, "$and");
    }

    private static Document madeQueryOR(Document dbObj1, Document dbObj2) {
        return madeQueryGeneral(dbObj1, dbObj2, "$or");
    }

    private static Document madeQueryGeneral(Document dbObj1, Document dbObj2, String condition) {
        Document query = new Document();
        List<Document> listObj = new ArrayList<Document>();
        listObj.add(dbObj1);
        listObj.add(dbObj2);

        query.put(condition, listObj);
        return query;
    }
}
