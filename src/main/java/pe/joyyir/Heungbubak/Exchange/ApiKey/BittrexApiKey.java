package pe.joyyir.Heungbubak.Exchange.ApiKey;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.IOUtil;

public class BittrexApiKey implements ApiKey {
    private static final String APIKEY_PATH = Config.getApikeyPathBittrex();

    @Getter @Setter
    private JSONObject apikey;

    public BittrexApiKey() throws Exception {
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

    public String getKey() {
        return getApikey().getString("Key");
    }

    public String getSecret() {
        return getApikey().getString("Secret");
    }
}
