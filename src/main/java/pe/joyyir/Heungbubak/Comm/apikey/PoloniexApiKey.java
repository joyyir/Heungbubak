package pe.joyyir.Heungbubak.Comm.apikey;

import pe.joyyir.Heungbubak.Util.Config;
import pe.joyyir.Heungbubak.Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class PoloniexApiKey implements ApiKey {
    private static final String APIKEY_PATH = Config.getApikeyPathPoloniex();

    @Getter @Setter
    private JSONObject apikey;

    public PoloniexApiKey() throws Exception {
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

    public String getKey() { return (String) getApikey().get("key"); }

    @Deprecated
    public int getNonce() {
        return (int) getApikey().get("nonce");
    }

    @Deprecated
    public void setNonce(int nonce) {
        getApikey().put("nonce", nonce);
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
