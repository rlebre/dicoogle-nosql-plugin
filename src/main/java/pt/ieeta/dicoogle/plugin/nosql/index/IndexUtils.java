package pt.ieeta.dicoogle.plugin.nosql.index;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.VR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class IndexUtils {
    private static final Logger logger = LoggerFactory.getLogger(NoSqlIndexPlugin.class);

    public static String getValue(DicomElement element) {
        if (!isBinaryField(element.vr())) {
            String value = null;
            Charset utf8charset = Charset.forName("UTF-8");
            Charset iso88591charset = Charset.forName("iso8859-1");
            byte[] values = element.getBytes();
            ByteBuffer inputBuffer = ByteBuffer.wrap(values);

            // decode UTF-8
            CharBuffer data = iso88591charset.decode(inputBuffer);

            // encode ISO-8559-1
            ByteBuffer outputBuffer = utf8charset.encode(data);

            byte[] outputData = outputBuffer.array();

            try {
                value = new String(outputData, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.error("ERROR: @TODO", ex);
            }

            return value;
        }

        if (element.vr() == VR.FD && element.getBytes().length == 8) {
            double tmpValue = element.getDouble(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.FL && element.getBytes().length == 4) {
            float tmpValue = element.getFloat(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.UL && element.getBytes().length == 4) {
            long tmpValue = element.getInt(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.US && element.getBytes().length == 2) {
            short[] tmpValue = element.getShorts(true);
            return String.valueOf(tmpValue[0]);
        }

        if (element.vr() != VR.US) {
            long tmpValue = byteArrayToInt(element.getBytes());
            return String.valueOf(tmpValue);
        }

        int tmpValue = element.getInt(true);

        return String.valueOf(tmpValue);
    }

    /**
     * Convert the byte array to an int.
     *
     * @param b The byte array
     * @return The integer
     */
    private static long byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b      The byte array
     * @param offset The array offset
     * @return The integer
     */
    private static long byteArrayToInt(byte[] b, int offset) {
        long value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    public static boolean isBinaryField(VR vr) {
        return vr == VR.SS || vr == VR.US || vr == VR.SL || vr == VR.UL || vr == VR.FL || vr == VR.FD;
    }
}
