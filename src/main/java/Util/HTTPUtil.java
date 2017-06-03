package Util;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
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

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

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

    public static void main(String[] args) {
        try {
            Map<String, String> map = new HashMap<>();
            map.put("name", "junyeong");
            map.put("age", "26");
            System.out.println(paramsBuilder(map));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
