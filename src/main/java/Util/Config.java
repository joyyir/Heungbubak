package Util;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class Config {
    private static final String CONFIG_PATH = "/Users/1003880/IdeaProjects/Heungbubak/src/main/resources/config.json";

    @Getter @Setter
    private static JSONObject config;

    static {
        try {
            readConfig(CONFIG_PATH);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void readConfig(String path) throws Exception {
        setConfig(IOUtil.readJson(path));
    }

    public static void saveConfig(String path) throws Exception {
        IOUtil.writeJson(path, getConfig());
    }

    public static String getApikeyPathCoinone() {
        return getConfig().getString("apikeyPathCoinone");
    }

    public static String getApikeyPathPoloniex() {
        return getConfig().getString("apikeyPathPoloniex");
    }

    public static String getApikeyPathKakao() {
        return getConfig().getString("apikeyPathKakao");
    }

    public static JSONObject getPaperWallet() {
        return getConfig().getJSONObject("paperWallet");
    }

    public static JSONObject getKakaoApi() {
        return getConfig().getJSONObject("kakaoAPI");
    }

    public static String getTargetEmail() {
        return getConfig().getString("targetEmail");
    }
}
