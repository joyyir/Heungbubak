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
        // 시세와 개수 가져옴
        long coinonePrice = 0,  bithumbPrice = 0,
             coinoneAmount = 0, bithumbAmount = 0;

        if(coinonePrice - bithumbPrice >= MIN_DIFF_BTC) {
            // 거래 가능 여부 확인 (충분한 BTC, KRW)

            // 코인원에서 비싸게 팔고, 빗썸에서 싸게 산다.

            // 코인원에서 BTC 판매 (-BTC, +KRW)

            // 빗썸에서 BTC 구매 (송금 수수료만큼 더 사야함)  (+BTC, -KRW)

            // 빗썸->코인원 BTC 송금

            // KRW 송금에 대한 노티
        }
        else if(bithumbPrice - coinonePrice >= MIN_DIFF_BTC) {
            // 빗썸에서 비싸게 팔고, 코인원에서 싸게 산다.
        }
    }
}
