package pe.joyyir.Heungbubak.Exchange.ApiKey;

import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class CoinoneApiKey implements ApiKey {
    private static final String APIKEY_PATH = Config.getApikeyPathCoinone();

    @Getter @Setter
    private JSONObject apikey;

    public CoinoneApiKey() throws Exception {
        readApiKey(APIKEY_PATH);
    }

    @Override
    public void readApiKey(String path) throws Exception {
        setApikey(IOUtil.readJson(path));
    }

    @Override
    public void saveApiKey(String path) throws Exception {
        IOUtil.writeJson(path, getApikey());
    }

    @Deprecated
    public long getNonce() {
        return getApikey().getLong("nonce");
    }

    @Deprecated
    public void setNonce(long nonce) {
        getApikey().put("nonce", nonce);
    }

    public String getAccessToken() {
        return (String) getApikey().get("access_token");
    }

    public String getSecret() {
        return (String) getApikey().get("secret");
    }

    @Deprecated
    public long getIncreasedNonce() throws Exception {
        long nonce = getNonce() + 1;
        setNonce(nonce);
        saveApiKey(APIKEY_PATH);
        return nonce;
    }
}
