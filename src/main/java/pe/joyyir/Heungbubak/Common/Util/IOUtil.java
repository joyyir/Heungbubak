package pe.joyyir.Heungbubak.Common.Util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IOUtil {
    public static String readFile(String filepath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            return sb.toString();
        } finally {
            br.close();
        }
    }

    public static void writeFile(String filepath, String body) throws Exception {
        FileOutputStream output = new FileOutputStream(filepath);
        output.write(body.getBytes("UTF-8"));
        output.close();
    }

    public static JSONObject readJson(String filepath) throws Exception {
        return new JSONObject(readFile(filepath));
    }

    public static void writeJson(String filepath, JSONObject jsonObject) throws Exception {
        writeFile(filepath, jsonObject.toString());
    }

    public static List<List<String>> readCsv(String filepath) throws Exception {
        String buf = readFile(filepath);
        List<List<String>> sheet = new ArrayList<>();
        List<String> rows = Arrays.asList(buf.split("\n"));
        if (CmnUtil.isNotEmpty(rows)) {
            for (String row : rows) {
                sheet.add(Arrays.asList(row.split(",")));
            }
        }
        return sheet;
    }

    public static void writeCsv(String filepath, List<List<String>> sheet) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (CmnUtil.isNotEmpty(sheet)) {
            for (List<String> list : sheet) {
                if (CmnUtil.isNotEmpty(list)) {
                    sb.append(String.join(",", list));
                    sb.append("\n");
                }
            }
        }
        writeFile(filepath, sb.toString());
    }

    public static void main(String[] args) {
        try {
            /*
            String filename = "myOrderHistory.csv";
            String filepath = IOUtil.class.getClassLoader().getResource(filename).getPath();
            List<List<String>> sheet = IOUtil.readCsv(filepath);
            sheet.toString();
            */

            /*
            String filename = "result.csv";
            String filepath = "/Users/1003880/" + filename;
            List<List<String>> sheet = new ArrayList<>();
            List<String> row1 = new ArrayList<>();
            row1.add("0-0");
            row1.add("0-1");
            row1.add("0-2");
            List<String> row2 = new ArrayList<>();
            row2.add("1-0");
            row2.add("1-1");
            sheet.add(row1);
            sheet.add(row2);
            writeCsv(filepath, sheet);
            */
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
