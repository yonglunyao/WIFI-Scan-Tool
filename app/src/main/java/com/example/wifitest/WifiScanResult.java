package com.example.wifitest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiScanResult {
    List<Map<String, Integer>> result = new ArrayList<>();

    public String toString() {
        Map<String, List<Integer>> map = new HashMap<>();
        for (Map<String, Integer> m : result) {
            for (String BSSID : m.keySet()) {
                if (!map.containsKey(BSSID)) {
                    List<Integer> list = new ArrayList<>();
                    map.put(BSSID, list);
                }
                map.get(BSSID).add(m.get(BSSID));
            }
        }

        String res = "";
        for (String BSSID : map.keySet()) {
            res = res + BSSID + "=" + map.get(BSSID).toString() + "\n";
        }
        return res;
    }

}
