package edu.illinois.confuzz.internal.types;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;

import java.util.LinkedList;
import java.util.List;

/**
 * List-like conf with delimiter , or ;
 * each element can have number, letter, =, - or _
 */
public class ListConf extends AbstractConfigType {
    private static final String[] delimiters = {",", ";"};
    private final int delimiterIdx;
    private final String defaultVal;
    private final String[] elements;

    public ListConf(String name, String val) {
        this.defaultVal = val;
        for (int i=0; i<delimiters.length; i++) {
            String[] elems = check(val, i, true);
            if (elems != null) {
                this.elements = elems;
                delimiterIdx = i;
                return;
            }
        }
        throw new GuidanceException(String.format("%s is not a list with value %s!", name, val));
    }


    @Override
    public String randomValue(Object value, List<Byte> bytes) {
        // TODO: might be longer than an integer here
        int bitset = toInteger(bytes);
        List<String> elemList = new LinkedList<>();
        for (int i=0; i<elements.length; i++) {
            if (bitset % 2 == 1) {
                elemList.add(elements[i]);
            }
            bitset >>= 1;
        }
        return String.join(delimiters[delimiterIdx], elemList);
    }

    @Override
    public int byteNum() {
        return 4;
    }

    @Override
    public Object getDefault() {
        return defaultVal;
    }

    public int getDelimiterIdx() {
        return delimiterIdx;
    }

    public static boolean check(String name, Object val) {
        if (val instanceof String) {
            for (int i=0; i<delimiters.length; i++) {
                if (check((String) val, i, true) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] check(String val, int idx, boolean checkLength) {
        String[] elems = val.split(delimiters[idx], -1);
        if (!checkLength && val.equals("")) {
            return elems;
        }
        if (!checkLength || elems.length > 2) {
            for (int i=0; i<elems.length; i++) {
                if (!elems[i].matches("[a-zA-Z0-9_\\-=]+")) {
                    return null;
                }
            }
            return elems;
        }
        return null;
    }
}
