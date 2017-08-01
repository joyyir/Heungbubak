package pe.joyyir.Heungbubak.Exchange.ApiKey;

import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;
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

    @Deprecated
    public long getNonce() {
        return (long) getApikey().get("nonce");
    }

    @Deprecated
    public void setNonce(long nonce) {
        getApikey().put("nonce", nonce);
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
