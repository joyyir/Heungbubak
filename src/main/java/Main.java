import Routine.PriceChangeRoutine;
import Routine.ValueChangeRoutine;

public class Main {

    public static void main(String[] args) {
        final int TIME_INTERVAL = 10000;
        boolean isFirst = false;

        try {
            ValueChangeRoutine valueChange = new ValueChangeRoutine();
            PriceChangeRoutine priceChange = new PriceChangeRoutine();

            while (true) {
                Thread.sleep(TIME_INTERVAL);
                valueChange.run();
                priceChange.run();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
