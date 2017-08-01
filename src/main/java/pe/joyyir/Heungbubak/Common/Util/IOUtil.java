package pe.joyyir.Heungbubak.Common.Util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

public class IOUtil {

    public static JSONObject readJson(String filepath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            return new JSONObject(sb.toString());
        } finally {
            br.close();
        }
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
