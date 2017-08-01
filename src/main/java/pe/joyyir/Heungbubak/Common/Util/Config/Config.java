package pe.joyyir.Heungbubak.Common.Util.Config;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Util.Config.Domain.ArbitrageConfigVO;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;

public class Config {
    private static final String CONFIG_FILE = "config.json";

    @Getter @Setter
    private static JSONObject config;

    static {
        try {
            readConfig(getResourcePath(CONFIG_FILE));
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

    public static String getResourcePath(String filename) {
        String path;
        String env = System.getProperty("env");
        if("dev".equals(env))
            path = Config.class.getClassLoader().getResource(filename).getPath();
        else
            path = "./resources/" + filename;
        return path;
    }

    public static String getApikeyPathCoinone() {
        return getResourcePath(config.getString("apikeyPathCoinone"));
    }

    public static String getApikeyPathPoloniex() {
        return getResourcePath(config.getString("apikeyPathPoloniex"));
    }

    public static String getApikeyPathBithumb() { return getResourcePath(config.getString("apikeyPathBithumb")); }

    public static String getApikeyPathKakao() {
        return getResourcePath(config.getString("apikeyPathKakao"));
    }

    public static JSONObject getPaperWallet() {
        return config.getJSONObject("paperWallet");
    }

    public static JSONObject getKakaoApi() {
        return config.getJSONObject("kakaoAPI");
    }

    public static String getTargetEmail() {
        return config.getString("targetEmail");
    }

    public static JSONArray getPreviousPrice() {
        return config.getJSONArray("previousPrice");
    }

    public static long getInvestment() {
        return config.getLong("investment");
    }

    public static ArbitrageConfigVO getArbitrageConfig() {
        ArbitrageConfigVO vo = new ArbitrageConfigVO();
        JSONObject json = config.getJSONObject("arbitrage");
        vo.setMinProfit(json.getLong("minProfit"));
        // TODO : 개발 해야함
        return null;
    }
}
