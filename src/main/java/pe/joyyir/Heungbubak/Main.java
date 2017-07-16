package pe.joyyir.Heungbubak;

import pe.joyyir.Heungbubak.Comm.EmailSender;
import pe.joyyir.Heungbubak.Routine.PriceChangeRoutine;
import pe.joyyir.Heungbubak.Routine.Routine;
import pe.joyyir.Heungbubak.Routine.ValueChangeRoutine;
import pe.joyyir.Heungbubak.Routine.ArbitrageRoutine;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        final int timeInterval = 10000;
        final String subject = "ÈïºÎ¹Ú ¾Ë¸²";
        List<Routine> routineList = new ArrayList();

        try {
            EmailSender sender = new EmailSender(subject);
            sender.setReady(false);

            routineList.add(new ValueChangeRoutine(sender));
            routineList.add(new PriceChangeRoutine(sender));
            routineList.add(new ArbitrageRoutine(sender));

            while (true) {
                Thread.sleep(timeInterval);

                for(Routine rt : routineList)
                    rt.run();

                if(sender.isReady()) {
                    sender.sendEmail();
                    sender.setReady(false);
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
