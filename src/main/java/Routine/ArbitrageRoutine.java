package Routine;

import Comm.BithumbComm;
import Comm.CoinoneComm;
import Comm.EmailSender;
import lombok.Setter;

import java.util.Scanner;

public class ArbitrageRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 3000;
    private final int MIN_DIFF_ETC = 300;
    private final int MIN_DIFF_XRP = 3;

    private final String BITHUMB_BTC_WALLET_ADDRESS = "1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM";
    private final String COINONE_BTC_WALLET_ADDRESS = "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59";
    private final String COINONE_BTC_WALLET_TYPE    = "trade";

    private CoinoneComm coinone = new CoinoneComm();
    private BithumbComm bithumb = new BithumbComm();

    private boolean isTiming = false;
    private int timingCount = 0;

    @Setter
    private EmailSender emailSender = null;

    public ArbitrageRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        try {
            // �ü��� ���� ������
            long coinoneAmount = 0, bithumbAmount = 0;
            long bithumbPrice = bithumb.getMarketPrice(BithumbComm.COIN_BTC, BithumbComm.PriceType.BUY);
            long coinonePrice = coinone.getMarketPrice(CoinoneComm.COIN_BTC);

            if(isTiming) { // �ѹ� �˸� �Ŀ��� 60�ʵ��� �ٽ� �˸��� ���� �ʴ´�.
                timingCount++;
                if(timingCount == 6) {
                    isTiming = false;
                    timingCount = 0;
                }
            }
            else if (Math.abs(coinonePrice - bithumbPrice) >= MIN_DIFF_BTC) {
                isTiming = true;

                String mailMsg =
                    "BTC �ŷ��� ���� �ŷ� Ÿ�̹� �Դϴ�.\n" +
                    "Bithumb: " + bithumbPrice + "\n" +
                    "Coinone: " + coinonePrice + "\n" +
                    "����: " + Math.abs(coinonePrice - bithumbPrice);
                emailSender.setStringAndReady("Arbitrage", mailMsg);
            }

            if (coinonePrice - bithumbPrice >= MIN_DIFF_BTC) {
                // �ŷ� ���� ���� Ȯ�� (����� BTC, KRW)

                // ���ο����� ��ΰ� �Ȱ�, ���濡�� �ΰ� ���.

                // ���ο����� BTC �Ǹ� (-BTC, +KRW)

                // ���濡�� BTC ���� (�۱� �����Ḹŭ �� �����)  (+BTC, -KRW)

                // ����->���ο� BTC �۱�

                // KRW �۱ݿ� ���� ��Ƽ
            } else if (bithumbPrice - coinonePrice >= MIN_DIFF_BTC) {
                // ���濡�� ��ΰ� �Ȱ�, ���ο����� �ΰ� ���.
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            BithumbComm bithumb = new BithumbComm();
            CoinoneComm coinone = new CoinoneComm();

            // step 1. �ü� Ȯ��
            long bithumbPrice, coinonePrice;
            bithumbPrice = bithumb.getMarketPrice(BithumbComm.COIN_BTC, BithumbComm.PriceType.BUY);
            coinonePrice = coinone.getMarketPrice(CoinoneComm.COIN_BTC);

            System.out.printf("step 1. �ü�\n");
            System.out.printf("\tbithumb(%d) coinone(%d) diff(%d)\n", bithumbPrice, coinonePrice, Math.abs(bithumbPrice-coinonePrice));

            // step 2. �ŷ� ���ɾ� ���� ���� Ȯ��
            double bithumbKRW, bithumbBTC;
            double coinoneKRW, coinoneBTC;
            bithumbKRW = bithumb.getBalance(BithumbComm.COIN_KRW);
            bithumbBTC = bithumb.getBalance(BithumbComm.COIN_BTC);
            coinoneKRW = coinone.getBalance(CoinoneComm.COIN_KRW);
            coinoneBTC = coinone.getBalance(CoinoneComm.COIN_BTC);

            System.out.printf("\nstep 2. ������\n");
            System.out.printf("\tbithumb: KRW(%f) BTC(%f)\n", bithumbKRW, bithumbBTC);
            System.out.printf("\tcoinone: KRW(%f) BTC(%f)\n", coinoneKRW, coinoneBTC);

            // step 3. �ŷ� ����
            // step 3-1. ���濡�� �Ȱ� ���ο����� ����
            System.out.printf("\nstep 3. �ŷ� ����\n");
            System.out.printf("\tstep 3-1. ���濡�� �Ȱ� ���ο����� ����\n");
            System.out.printf("\t\t���濡�� �� �� �ִ� ����: %f\n", bithumbBTC);
            System.out.printf("\t\t���ο����� �� �� �ִ� ����: %f\n", coinoneKRW/coinonePrice);
            System.out.printf("\t\t���� �ŷ��� �� �ִ� ����: %f\n", Math.min(bithumbBTC, coinoneKRW/coinonePrice));
            System.out.printf("\tstep 3-2. ���ο����� �Ȱ� ���濡�� ����\n");
            System.out.printf("\t\t���ο����� �� �� �ִ� ����: %f\n", coinoneBTC);
            System.out.printf("\t\t���濡�� �� �� �ִ� ����: %f\n", bithumbKRW/bithumbPrice);
            System.out.printf("\t\t���� �ŷ��� �� �ִ� ����: %f\n", Math.min(coinoneBTC, bithumbKRW/bithumbPrice));

            // step 4. �ŷ� ����
            System.out.printf("\nstep 4. �ŷ� ����\n");
            System.out.printf("\t�ŷ��� �����Ϸ� �մϴ�. �����Ͻʴϱ�?(y/n) : ");
            Scanner sc = new Scanner(System.in);
            String userInput = sc.nextLine();
            if(!userInput.toUpperCase().equals("Y"))
                return;

            // step 5. �Ǹ�

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
