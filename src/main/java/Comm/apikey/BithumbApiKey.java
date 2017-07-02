package Comm.apikey;

import Util.Config;
import Util.IOUtil;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class BithumbApiKey implements ApiKey {
    private static final String APIKEY_PATH = Config.getApikeyPathBithumb();

    @Getter @Setter
    private JSONObject apikey;

    public BithumbApiKey() throws Exception {
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

    public String getKey() { return (String) getApikey().get("connect"); }

    public long getNonce() {
        return (long) getApikey().get("nonce");
    }

    public void setNonce(long nonce) {
        getApikey().put("nonce", nonce);
    }

    public String getSecret() {
        return (String) getApikey().get("secret");
    }

    public long getIncreasedNonce() throws Exception {
        long nonce = getNonce() + 1;
        setNonce(nonce);
        saveApiKey(APIKEY_PATH);
        return nonce;
    }
}
