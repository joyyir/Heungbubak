package pe.joyyir.Heungbubak.Common.Util.Config;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Util.Config.Domain.ArbitrageConfigVO;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;

import java.util.*;

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

    public static String[] getTargetEmail() {
        JSONArray array = config.getJSONArray("targetEmail");
        String[] emails = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            emails[i] = array.getString(i);
        }
        return emails;
    }

    public static JSONArray getPreviousPrice() {
        return config.getJSONArray("previousPrice");
    }

    public static long getInvestment() {
        return config.getLong("investment");
    }

    public static ArbitrageConfigVO getArbitrageConfig() throws Exception {
        ArbitrageConfigVO vo = new ArbitrageConfigVO();
        JSONObject arbitrage = config.getJSONObject("arbitrage");
        JSONObject minDiff = arbitrage.getJSONObject("minDiff");
        JSONArray targetCoin = arbitrage.getJSONArray("targetCoin");

        Iterator it = minDiff.keys();
        Map<Coin, Long> minDiffMap = new HashMap<>();
        while (it.hasNext()) {
            String coinStr = ((String) it.next()).toUpperCase();
            Coin coin;
            try {
                coin = Coin.valueOf(coinStr);
            }
            catch (Exception e) {
                e.printStackTrace(System.out);
                continue;
            }
            long diff = minDiff.getLong(coinStr);
            minDiffMap.put(coin, diff);
        }

        List<Coin> targetCoinArr = new ArrayList<>();
        for(int i = 0; i < targetCoin.length(); i++) {
            String coinStr = targetCoin.getString(i).toUpperCase();
            Coin coin;
            try {
                coin = Coin.valueOf(coinStr);
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            targetCoinArr.add(coin);
        }

        vo.setMinProfit(arbitrage.getLong("minProfit"));
        vo.setMinDiffMap(minDiffMap);
        vo.setTargetCoin(targetCoinArr);
        vo.setMaxLoss(arbitrage.getLong("maxLoss"));
        vo.setMaxWaitingSec(arbitrage.getLong("maxWaitingSec"));
        vo.setReverseDiffXRP(arbitrage.getLong("reverseDiffXRP"));
        vo.setQtyMultiplyNum(arbitrage.getDouble("qtyMultiplyNum"));
        vo.setPriceDiffXRP(arbitrage.getLong("priceDiffXRP"));

        if (vo.getMinProfit() < 1000)
            throw new Exception("Too low minProfit: " + vo.getMinProfit());
        if (vo.getReverseDiffXRP() > 5)
            throw new Exception("Too high reverseDiffXRP: " + vo.getReverseDiffXRP());
        if (vo.getQtyMultiplyNum() < 1)
            throw new Exception("Too low qtyMultiplyNum: " + vo.getQtyMultiplyNum());
        if (vo.getPriceDiffXRP() > 10)
            throw new Exception("Too high priceDiffXRP: " + vo.getPriceDiffXRP());

        return vo;
    }
}
