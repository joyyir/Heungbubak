package pe.joyyir.Heungbubak.Comm;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailSender {
    private List<String> strList = new ArrayList<>();
    private Map<String, String> strMap = new HashMap<>();
    private String subject = "undefined";
    @Getter @Setter
    private boolean isReady = true;

    public EmailSender(String subject) {
        if(subject != null)
            this.subject = subject;
    }

    public void setString(String key, String value) {
        if(key == null || value == null)
            return;

        if(strMap.get(key) == null)
            strList.add(value);
        strMap.put(key, value);
    }

    public void sendEmail() throws Exception {
        GmailComm.sendEmail(subject, getEmailMessage());
    }

    public String getEmailMessage() {
        if(strList == null || strList.size() < 1)
            return "undefined";

        StringBuilder sb = new StringBuilder();

        for(String str : strList) {
            sb.append(str);
            sb.append("\n\n");
        }

        return sb.toString();
    }

    public void setStringAndReady(String key, String value) {
        setString(key, value);
        setReady(true);
    }
}
