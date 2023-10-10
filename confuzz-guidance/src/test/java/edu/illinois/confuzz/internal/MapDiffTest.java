package edu.illinois.confuzz.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapDiffTest {

    @Test
    public void testMapDiff() {
        // Call ConfigUtils.getMapDiffMsg to get the difference between two map
        Map<String, String> map1 = new LinkedHashMap<>();
        Map<String, String> map2 = new LinkedHashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map1.put("c", "3");
        map1.put("e", "4");
        map2.put("a", "1");
        map2.put("b", "2");
        map2.put("c", "5");
        map2.put("d", "4");
        Map<String, String> diff = ConfigUtils.getMapDiff(map1, map2);
        Assert.assertEquals(diff.size(), 3);
        Assert.assertTrue(diff.get("c").equals("5"));
        Assert.assertTrue(diff.get("d").equals("4"));
        Assert.assertTrue(diff.get("e").equals("4"));
        String diffMsg = ConfigUtils.getMapDiffMsg(map1, map2);
        System.out.println(diffMsg);

    }
}
