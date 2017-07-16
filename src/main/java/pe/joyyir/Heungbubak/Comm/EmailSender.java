package pe.joyyir.Heungbubak.Comm;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailSender {
    private List<String> strList = new ArrayList<>();
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

        strList.add(value);
    }

    public void sendEmail() throws Exception {
        GmailComm.sendEmail(subject, getEmailMessage());
        strList.clear();
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
