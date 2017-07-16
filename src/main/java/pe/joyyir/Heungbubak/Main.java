package pe.joyyir.Heungbubak;

import pe.joyyir.Heungbubak.Comm.EmailSender;
import pe.joyyir.Heungbubak.Routine.PriceChangeRoutine;
import pe.joyyir.Heungbubak.Routine.Routine;
import pe.joyyir.Heungbubak.Routine.ValueChangeRoutine;
import pe.joyyir.Heungbubak.Routine.ArbitrageRoutine;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int DEFAULT_TIME_INTERVAL = 10000;

    public static void main(String[] args) {
        int timeInterval = DEFAULT_TIME_INTERVAL;
        final String subject = "흥부박 알림";

        if(args.length > 0)
            timeInterval = Integer.valueOf(args[0]) * 1000;

        List<Routine> routineList = new ArrayList();

        try {
            EmailSender sender = new EmailSender(subject);
            sender.setReady(false);

            //routineList.add(new ValueChangeRoutine(sender));
            //routineList.add(new PriceChangeRoutine(sender));
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
