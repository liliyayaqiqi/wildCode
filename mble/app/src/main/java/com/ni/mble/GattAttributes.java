package com.ni.mble;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String NI_MBLE_AUX_SERVICE = "da5f119c-9048-33a7-e94e-0e32b892f709";
    public static String NI_MBLE_MEAS_SERVICE = "0d4231ce-03ae-44b7-85f4-ccea6dd7ac88";
    public static String NI_MBLE_SN_READ = "da5fb0e0-9048-33a7-e94e-0e32b892f709";
    public static String NI_MBLE_MEAS_CONFIG_WAVEFORM = "0d421670-03ae-44b7-85f4-ccea6dd7ac88";
    public static String NI_MBLE_MEAS_CONFIG_START = "0d42125a-03ae-44b7-85f4-ccea6dd7ac88";
    public static String NI_MBLE_MEAS_TRANS_CONTROL = "0d42a4cf-03ae-44b7-85f4-ccea6dd7ac88";
    public static String NI_MBLE_MEAS_TRANS_DATA = "0d420451-03ae-44b7-85f4-ccea6dd7ac88";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

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