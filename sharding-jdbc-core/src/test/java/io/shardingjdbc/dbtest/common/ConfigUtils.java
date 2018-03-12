package io.shardingjdbc.dbtest.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigUtils {

    private static Properties config = null;


    static {
        try {
            config = new Properties();
            try{
                config.load(ConfigUtils.class.getClassLoader().getResourceAsStream("integrate/env.properties"));
            }catch (Exception e){
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String getString(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    /**
     *
     * @param startKey
     * @return
     */
    public static Map<String,String> getDatas(String startKey){
        Map<String,String> maps = new HashMap();
        for (Map.Entry<Object, Object> objectObjectEntry : config.entrySet()) {
            String key = (String)objectObjectEntry.getKey();
            if(key.startsWith(startKey)){
                maps.put(key,(String)objectObjectEntry.getValue());
            }
        }
        return maps;
    }


}
