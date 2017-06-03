package Comm;

import Util.HTTPUtil;
import Util.IOUtil;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 1003880 on 2017. 5. 3..
 */
public class KakaoComm {
    private final String AUTH_URL = "https://kauth.kakao.com";
    private final String API_URL = "https://kapi.kakao.com";

    private void dummy() {
        try {
            String apiKey = (String) ((JSONObject)IOUtil.getConfig("kakaoAPI")).get("restApiKey");
            String url = AUTH_URL + "oauth/authorize?client_id=" + apiKey + "&redirect_uri=" + "http://localhost/" + "&response_type=code";
            String result = HTTPUtil.requestGet(url, "*");
            System.out.println("result: " + result);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject getUserToken(String authCode) throws Exception {
        String apiKey = (String) ((JSONObject)IOUtil.getConfig("kakaoAPI")).get("restApiKey");

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", apiKey);
        params.put("redirect_uri", "http://localhost/oauth.jsp");
        params.put("code", authCode);

        Map<String, String> reqProps = new HashMap<>();
        reqProps.put("Accept", "application/json");

        return HTTPUtil.getJSONfromPost(AUTH_URL + "/oauth/token", reqProps, HTTPUtil.paramsBuilder(params));
    }

    // TODO : 토큰 갱신 로직 추가

    public String getAccessToken() throws Exception {
        JSONObject config = IOUtil.getConfig();
        JSONObject kakaoJson = IOUtil.readJson((String)config.get("jsonPath") + config.get("kakaoJson"));
        return (String) kakaoJson.get("access_token");
    }

    public void sendMessage(String title, String desc) throws Exception {
        String accessToken = getAccessToken();
        Map<String, String> reqProps = new HashMap<>();
        reqProps.put("Authorization", "Bearer " + accessToken);
        reqProps.put("Content-Type", "application/x-www-form-urlencoded");

        /*
        String reqUrl = "/v2/api/talk/memo/default/send";

        JSONObject link = new JSONObject();
        link.put("web_url", "");

        JSONObject content = new JSONObject();
        content.put("title", msg);
        content.put("image_url", "");
        content.put("link", link);

        JSONObject feed = new JSONObject();
        feed.put("object_type", "feed");
        feed.put("content", content);

        String params = "template_object=" + feed.toString();
        */

        String reqUrl = "/v2/api/talk/memo/send";

        JSONObject args = new JSONObject();
        args.put("title", title);
        args.put("description", desc);

        String params = "template_id=3818&args=" + args.toString();

        JSONObject result = HTTPUtil.getJSONfromPost(API_URL + reqUrl, reqProps, params);
        if( (Integer)result.get("result_code") != 0) {
            throw new Exception();
        }
    }

    public static void main(String[] args) {
        try {
            new KakaoComm().sendMessage("this is title", "this is desc");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
