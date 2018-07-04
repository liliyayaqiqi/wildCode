package com.ni.mble;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String NI_MBLE_AUX_SERVICE = "09f792b8-320e-4ee9-a733-48909c1105fda";
    public static String NI_MBLE_SN_READ = "09f792b8-320e-4ee9-a733-4890e0b05fda";

    static {
        // NI MBLE services.
        attributes.put(NI_MBLE_AUX_SERVICE, "NI Mble auxiliary service");
        // NI MBLE Characteristics.
        attributes.put(NI_MBLE_SN_READ, "Serial Number");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}