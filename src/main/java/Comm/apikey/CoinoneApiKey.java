package Comm.apikey;

import Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class CoinoneApiKey implements ApiKey {
    private static final String APIKEY_PATH = "/Users/1003880/IdeaProjects/Heungbubak/src/main/resources/apiKeyCoinone.json";

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

    public int getNonce() {
        return (int) getApikey().get("nonce");
    }

    public void setNonce(int nonce) {
        getApikey().put("nonce", nonce);
    }

    public String getAccessToken() {
        return (String) getApikey().get("access_token");
    }

    public String getSecret() {
        return (String) getApikey().get("secret");
    }

    public int getIncreasedNonce() throws Exception {
        int nonce = getNonce() + 1;
        setNonce(nonce);
        saveApiKey(APIKEY_PATH);
        return nonce;
    }
}
