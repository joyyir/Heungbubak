package Routine;

public class ArbitrageRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 3000;
    private final int MIN_DIFF_ETC = 300;
    private final int MIN_DIFF_XRP = 3;

    private final String BITHUMB_BTC_WALLET_ADDRESS = "1AKnnChADG5svVrNbAGnF4xdNdZ515J4oM";
    private final String COINONE_BTC_WALLET_ADDRESS = "1GdHw2mKCH6scrYvpR6NFikJqthyn6ee59";
    private final String COINONE_BTC_WALLET_TYPE    = "trade";

    @Override
    public void run() {
        // �ü��� ���� ������
        long coinonePrice = 0,  bithumbPrice = 0,
             coinoneAmount = 0, bithumbAmount = 0;

        if(coinonePrice - bithumbPrice >= MIN_DIFF_BTC) {
            // �ŷ� ���� ���� Ȯ�� (����� BTC, KRW)

            // ���ο����� ��ΰ� �Ȱ�, ���濡�� �ΰ� ���.

            // ���ο����� BTC �Ǹ� (-BTC, +KRW)

            // ���濡�� BTC ���� (�۱� �����Ḹŭ �� �����)  (+BTC, -KRW)

            // ����->���ο� BTC �۱�

            // KRW �۱ݿ� ���� ��Ƽ
        }
        else if(bithumbPrice - coinonePrice >= MIN_DIFF_BTC) {
            // ���濡�� ��ΰ� �Ȱ�, ���ο����� �ΰ� ���.
        }
    }
}
