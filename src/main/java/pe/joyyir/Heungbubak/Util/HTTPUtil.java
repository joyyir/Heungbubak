package pe.joyyir.Heungbubak.Util;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by 1003880 on 2017. 5. 1..
 */
public class HTTPUtil {
    @Getter
    @Setter
    private static SSLSocketFactory sslSocketFactory = null;

    static {
            /*
            System.setProperty("javax.net.ssl.keyStore", "/Users/1003880/Programs/secure/heungbubak");
            System.setProperty("javax.net.ssl.keyStorePassword", "heungbubak");
            //System.setProperty("javax.net.debug", "ssl");

            //System.setProperty("javax.net.ssl.trustStore", "/Users/1003880/Programs/secure/cacerts");
            //System.setProperty("javax.net.ssl.trustStorePassword", "heungbubak");

            System.out.println("*********** keyStore : " + System.getProperty("javax.net.ssl.keyStore"));
            System.out.println("*********** trustStore : " + System.getProperty("javax.net.ssl.trustStore"));

            setSslSocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault());
            */
    }

    public static String requestGet(String strUrl, String accept) throws Exception {
        URL url = new URL(strUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", accept);
        conn.setConnectTimeout(5000);
        //conn.setSSLSocketFactory(getSslSocketFactory());
        conn.connect();

        if(conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
//            throw new Exception();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public static String requestPost(String strUrl, Map<String, String> reqProps, String params) throws Exception {
        URL url = new URL(strUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        if(reqProps != null)
            for(String key : reqProps.keySet()) {
                conn.setRequestProperty(key, reqProps.get(key));
            }
        //conn.setSSLSocketFactory(getSslSocketFactory());
        conn.setConnectTimeout(5000);

        params = (params == null ? "" : params);
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();

        InputStream inputStream = (conn.getResponseCode() == 200) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        if(conn.getResponseCode() != 200)
            throw new Exception("error: " + conn.getResponseCode() + ", message: " + response.toString());

        return response.toString();
    }

    public static JSONObject getJSONfromGet(String apiUrl) throws Exception {
        return new JSONObject(requestGet(apiUrl, "application/json"));
    }

    public static JSONObject getJSONfromPost(String apiUrl, Map<String, String> reqProps, String params) throws Exception {
        return new JSONObject(requestPost(apiUrl, reqProps, params));
    }

    public static String paramsBuilder(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        for(String key : map.keySet()) {
            builder.append(key);
            builder.append("=");
            builder.append(map.get(key));
            builder.append("&");
        }
        builder.deleteCharAt(builder.length()-1);
        return builder.toString();
    }

    public static String encodeURIComponent(String s) {
        String result;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%26", "&")
                    .replaceAll("\\%3D", "=")
                    .replaceAll("\\%7E", "~");
        }
        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    public static void main(String[] args) {
        try {
            for(int i = 0; i < 1000; i++) {
                System.out.println(CmnUtil.nsTime());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
