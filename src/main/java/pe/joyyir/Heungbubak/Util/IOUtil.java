package pe.joyyir.Heungbubak.Util;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class IOUtil {

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
}
