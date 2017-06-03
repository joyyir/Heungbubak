package Util;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by 1003880 on 2017. 5. 1..
 */
public class IOUtil {
    private static final String CONFIG_PATH = "/Users/1003880/IdeaProjects/Heungbubak/src/main/resources/config.json";

    @Setter
    private static JSONObject config = null;

    public static JSONObject readJson(String filepath) throws Exception {
        byte[] b = new byte[1024];
        FileInputStream input = new FileInputStream(filepath);
        input.read(b);
        input.close();
        return new JSONObject(new String(b));
    }

    public static void writeJson(String filepath, JSONObject jsonObject) throws Exception {
        FileOutputStream output = new FileOutputStream(filepath);
        output.write(jsonObject.toString().getBytes("UTF-8"));
        output.close();
    }

    public static void main(String[] args) {
        try {

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getConfig() throws Exception {
        setConfig(readJson(CONFIG_PATH));
        return config;
    }

    public static Object getConfig(String key) throws Exception {
        setConfig(readJson(CONFIG_PATH));
        return getConfig().get(key);
    }
}
