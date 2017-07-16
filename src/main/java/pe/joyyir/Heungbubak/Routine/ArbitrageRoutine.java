package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Comm.BithumbComm;
import pe.joyyir.Heungbubak.Comm.CoinoneComm;
import pe.joyyir.Heungbubak.Comm.EmailSender;
import pe.joyyir.Heungbubak.Const.Coin;
import pe.joyyir.Heungbubak.Const.PriceType;
import lombok.Setter;

import java.util.Scanner;

public class ArbitrageRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 3000;
    private final int MIN_DIFF_ETC = 130;
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
            // config
            final Coin coin = Coin.ETC;
            final int MIN_DIFF = MIN_DIFF_ETC;

            boolean needNotice = false;
            long bithumbBuyPrice = bithumb.getMarketPrice(coin, PriceType.BUY);
            long bithumbSellPrice = bithumb.getMarketPrice(coin, PriceType.SELL);
            long coinoneBuyPrice = coinone.getMarketPrice(coin, PriceType.BUY);
            long coinoneSellPrice = coinone.getMarketPrice(coin, PriceType.SELL);
            long coinonePrice = 0, bithumbPrice = 0;

            if(isTiming) { // �ѹ� �˸� �Ŀ��� 60�ʵ��� �ٽ� �˸��� ���� �ʴ´�.
                timingCount++;
                if(timingCount == 6) {
                    isTiming = false;
                    timingCount = 0;
                }
            }
            else if (coinoneSellPrice - bithumbBuyPrice >= MIN_DIFF) {
                isTiming = true;
                needNotice = true;
                coinonePrice = coinoneSellPrice;
                bithumbPrice = bithumbBuyPrice;
            }
            else if (bithumbSellPrice - coinoneBuyPrice >= MIN_DIFF) {
                isTiming = true;
                needNotice = true;
                coinonePrice = coinoneBuyPrice;
                bithumbPrice = bithumbSellPrice;
            }

            if(needNotice) {
                String mailMsg =
                    coin.name() + " �ŷ��� ���� �ŷ� Ÿ�̹� �Դϴ�.\n" +
                    "Bithumb: " + bithumbPrice + "\n" +
                    "Coinone: " + coinonePrice + "\n" +
                    "����: " + Math.abs(bithumbBuyPrice - coinoneSellPrice);
                emailSender.setStringAndReady("Arbitrage", mailMsg);
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
            bithumbPrice = bithumb.getMarketPrice(Coin.BTC, PriceType.BUY);
            coinonePrice = coinone.getLastMarketPrice(Coin.BTC);

            System.out.printf("step 1. �ü�\n");
            System.out.printf("\tbithumb(%d) coinone(%d) diff(%d)\n", bithumbPrice, coinonePrice, Math.abs(bithumbPrice-coinonePrice));

            // step 2. �ŷ� ���ɾ� ���� ���� Ȯ��
            double bithumbKRW, bithumbBTC;
            double coinoneKRW, coinoneBTC;
            bithumbKRW = bithumb.getBalance(Coin.KRW);
            bithumbBTC = bithumb.getBalance(Coin.BTC);
            coinoneKRW = coinone.getBalance(Coin.KRW);
            coinoneBTC = coinone.getBalance(Coin.BTC);

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
