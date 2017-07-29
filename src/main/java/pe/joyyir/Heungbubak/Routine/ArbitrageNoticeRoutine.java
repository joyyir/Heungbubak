package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageExchange;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageMarketPrice;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.ArbitrageTrade;
import pe.joyyir.Heungbubak.Exchange.Arbitrage.DummyTrade;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import lombok.Setter;
import pe.joyyir.Heungbubak.Exchange.Service.BithumbService;
import pe.joyyir.Heungbubak.Exchange.Service.CoinoneService;
import pe.joyyir.Heungbubak.Common.Util.EmailSender;

public class ArbitrageNoticeRoutine implements Routine{
    private final int MIN_DIFF_BTC = 20000;
    private final int MIN_DIFF_ETH = 2000;
    private final int MIN_DIFF_ETC = 200;//100;
    private final int MIN_DIFF_XRP = 2;

    private final Coin[] COIN_ARR = {Coin.BTC, Coin.ETC, Coin.ETH, Coin.XRP};
    private final int[] DIFF_ARR = {MIN_DIFF_BTC, MIN_DIFF_ETC, MIN_DIFF_ETH, MIN_DIFF_XRP};
    private boolean[] canNoticeArr = {true, true, true, true};
    private int[] noticeCountArr = {0, 0, 0, 0};

    private CoinoneService coinone = new CoinoneService();
    private BithumbService bithumb = new BithumbService();

    @Setter
    private EmailSender emailSender = null;

    public ArbitrageNoticeRoutine(EmailSender emailSender) throws Exception {
        this.emailSender = emailSender;
    }

    @Override
    public void run() {
        String mailMsg = "";
        String mailSubject = "";

        for(int i = 0; i < COIN_ARR.length; i++) {
            try {
                final Coin coin = COIN_ARR[i];
                final int minDiff = DIFF_ARR[i];

                boolean isTiming = false, mustSellBithumb = false;
                long bithumbBuyPrice = bithumb.getMarketPrice(coin, PriceType.BUY);
                long bithumbSellPrice = bithumb.getMarketPrice(coin, PriceType.SELL);
                long coinoneBuyPrice = coinone.getMarketPrice(coin, PriceType.BUY);
                long coinoneSellPrice = coinone.getMarketPrice(coin, PriceType.SELL);
                long coinonePrice = 0, bithumbPrice = 0;

                System.out.printf("%s\n", coin.name());
                System.out.printf("[Bithumb] Buy: %d, Sell: %d\n", bithumbBuyPrice, bithumbSellPrice);
                System.out.printf("[Coinone] Buy: %d, Sell: %d\n", coinoneBuyPrice, coinoneSellPrice);
                System.out.printf("[현재 차익] %d\n", Math.max(bithumbBuyPrice - coinoneSellPrice, coinoneBuyPrice - bithumbSellPrice));

                if (!canNoticeArr[i]) {
                    noticeCountArr[i]++;
                    if (noticeCountArr[i] == 6) {
                        noticeCountArr[i] = 0;
                        canNoticeArr[i] = true;
                    }
                }

                if (bithumbBuyPrice - coinoneSellPrice >= minDiff) {
                    isTiming = true;
                    mustSellBithumb = true;
                    coinonePrice = coinoneSellPrice;
                    bithumbPrice = bithumbBuyPrice;
                } else if (coinoneBuyPrice - bithumbSellPrice >= minDiff) {
                    isTiming = true;
                    mustSellBithumb = false;
                    coinonePrice = coinoneBuyPrice;
                    bithumbPrice = bithumbSellPrice;
                }

                if (isTiming && canNoticeArr[i]) {
                    mailSubject += coin.name() + " ";
                    mailMsg +=
                            coin.name() + " 거래소 차익 거래 타이밍 입니다.\n" +
                                    (mustSellBithumb ? "빗썸에서 팔고 코인원에서 사세요.\n" : "코인원에서 팔고 빗썸에서 사세요.\n") +
                                    "Bithumb: " + bithumbPrice + "\n" +
                                    "Coinone: " + coinonePrice + "\n" +
                                    "차익: " + Math.abs(bithumbPrice - coinonePrice) + "\n";

                    canNoticeArr[i] = false;
                }

                System.out.printf("--------------------------------------------------\n");
            }
            catch (Exception e) {
                System.out.println("코인 시세 받아오던 중 오류 발생");
            }
        }

        if (emailSender != null && !"".equals(mailMsg)) {
            mailSubject += "타이밍";
            emailSender.setSubject(mailSubject);
            emailSender.setStringAndReady("Arbitrage", mailMsg);
            System.out.println(mailMsg);
        }

    }
}
