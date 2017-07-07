package Routine;

import Comm.BithumbComm;
import Comm.CoinoneComm;
import Comm.EmailSender;
import lombok.Setter;

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
}
