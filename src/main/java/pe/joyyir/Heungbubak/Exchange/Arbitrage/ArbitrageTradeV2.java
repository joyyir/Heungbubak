package pe.joyyir.Heungbubak.Exchange.Arbitrage;

import lombok.Getter;
import lombok.Setter;
import pe.joyyir.Heungbubak.Common.Const.Coin;
import pe.joyyir.Heungbubak.Common.Const.OrderType;
import pe.joyyir.Heungbubak.Common.Const.PriceType;
import pe.joyyir.Heungbubak.Common.Util.CmnUtil;
import pe.joyyir.Heungbubak.Common.Util.Config.Config;
import pe.joyyir.Heungbubak.Common.Util.Config.Domain.ArbitrageConfigVO;
import pe.joyyir.Heungbubak.Common.Util.EmailSender;

import java.util.Date;

public class ArbitrageTradeV2 implements Runnable {
    // parameters
    final int TRIAL_TIME_INTERVAL = 1000;

    public enum TradeStatus {
        START, ORDER_MADE, ORDER_CANCELED, ORDER_COMPLETED, ORDER_CANCEL_FAILED
    }
    @Getter @Setter
    private ArbitrageExchange exchange;
    @Getter @Setter
    private OrderType orderType;
    @Getter @Setter
    private Coin coin;
    @Getter @Setter
    private long price;
    @Getter @Setter
    private double quantity;
    @Getter @Setter
    ArbitrageTradeV2 oppositeTrade;
    @Getter @Setter
    String orderId;
    @Getter
    private Thread thread;

    private double beforeKrwBalance;
    private double beforeCoinBalance;

    @Setter
    private StringBuilder emailStringBuilder = null;

    // 동시성 제어
    private ArbitrageSharedResource sharedResource;
    private ArbitrageSharedResource.SharedResource myResource;

    //private long maxLoss;
    private long maxWaitingSec;
    private long reverseDiffXRP;

    // Custom setter & getter - start
    public void setTradeStatus(TradeStatus status) {
        synchronized (sharedResource) {
            myResource.tradeStatus = status;
            sharedResource.notify();
            if(getTradeStatus() != null) {
                //log("notify");
            }
        }
    }

    public TradeStatus getTradeStatus() {
        return myResource.tradeStatus;
    }

    public Boolean isCancelRequired() {
        synchronized (sharedResource) {
            return myResource.isCancelRequired;
        }
    }

    public void setCancelRequried(boolean cancelRequried) {
        myResource.isCancelRequired = cancelRequried;
    }

    public void setIsCancelRequired(boolean cancelRequired, String cause) {
        cause = (cause != null && !"".equals(cause)) ? cause : "불명";
        synchronized (sharedResource) {
            if (cancelRequired) {
                //log("거래 취소가 요청됨 (원인: " + cause + ")");
            }
            setCancelRequried(cancelRequired);
        }
    }

    boolean isTradeCompleted(TradeStatus tradeStatus) {
        return  tradeStatus == TradeStatus.ORDER_COMPLETED ||
                tradeStatus == TradeStatus.ORDER_CANCELED ||
                tradeStatus == TradeStatus.ORDER_CANCEL_FAILED;
    }
    // Custom setter & getter - end

    private void log(String str) {
        String indention = orderType == OrderType.SELL ? " " : "\t\t\t\t\t\t\t\t\t\t\t\t\t";
        String logStr = String.format("%s%s[%s] %s\n", CmnUtil.timeToString(new Date()), indention, orderType.name(), str);
        if(emailStringBuilder != null) {
            emailStringBuilder.append(logStr);
        }
        System.out.printf(logStr);
    }

    // Constructor
    public ArbitrageTradeV2(ArbitrageExchange exchange, OrderType orderType, Coin coin, long price, double quantity, double beforeKrwBal, double beforeCoinBal, ArbitrageSharedResource sharedResource) throws Exception {
        this.orderType = orderType;
        this.exchange = exchange;
        this.coin = coin;
        this.price = price;
        this.quantity = quantity;
        this.oppositeTrade = null;
        this.orderId = null;
        this.thread = new Thread(this);
        this.beforeCoinBalance = beforeCoinBal;
        this.beforeKrwBalance = beforeKrwBal;
        this.sharedResource = sharedResource;
        this.sharedResource.setResource(orderType, TradeStatus.START, false);
        this.myResource = sharedResource.getResource(orderType);

        ArbitrageConfigVO configVo = Config.getArbitrageConfig();
        //this.maxLoss = configVo.getMaxLoss();
        this.maxWaitingSec = configVo.getMaxWaitingSec();
        this.reverseDiffXRP = configVo.getReverseDiffXRP();
    }

    // Trade - start
    private void makeOrder() throws Exception {
        boolean isSuccess = false;
        Exception finalException = null;
        int count = 0;
        EmailSender emailSender = new EmailSender("흥부박 오류 알림");

        //for (int trial = 0; trial < maxWaitingSec; trial++) {
        while (true) {
            synchronized (sharedResource) {
                try {
                    orderId = exchange.makeOrder(orderType, coin, price, quantity);
                    log("거래 생성 완료");
                    setTradeStatus(TradeStatus.ORDER_MADE);
                    sharedResource.wait();
                    isSuccess = true;
                    break;
                } catch (Exception e) {
                    finalException = e;
                }
                try {
                    count++;
                    if (count % 1000 == 0) {
                        emailSender.setStringAndReady("", exchange.getExchangeName() + "에서 거래 생성 " + count + "번째 실패... 확인 필요");
                        emailSender.sendEmail();
                    }
                    Thread.sleep(TRIAL_TIME_INTERVAL);
                }
                catch (Exception e) {}
            }
        }

        if (!isSuccess) {
            throw new Exception("거래 생성 실패 " + ((finalException == null) ? "" : finalException));
        }
    }

    private void waitOrderCompleted(String orderId, OrderType orderType, Coin coin) throws Exception {
        boolean isSuccess = false;
        Exception finalException = null;

        for (int trial = 0; trial < maxWaitingSec; trial++) {
            synchronized (sharedResource) {
                try {
                    if (exchange.isOrderCompleted(orderId, orderType, coin)) {
                        log("거래 성공");
                        setTradeStatus(TradeStatus.ORDER_COMPLETED);
                        isSuccess = true;
                        break;
                    }
                } catch (Exception e) {
                    finalException = e;
                }
            }
            try {
                Thread.sleep(TRIAL_TIME_INTERVAL);
            }
            catch (InterruptedException e) {}
        }

        if (!isSuccess) {
            throw new Exception("거래가 제한 시간 내에 성사되지 않았습니다. " + ((finalException == null) ? "" : finalException));
        }
    }

    private void tryReverseOrder() throws Exception{
        try {
            double tradeQuantity;
            OrderType reversedOrderType = (orderType == OrderType.BUY) ? OrderType.SELL : OrderType.BUY;
            PriceType reversedPriceType = (orderType == OrderType.BUY) ? PriceType.BUY : PriceType.SELL;
            double afterCoinBalance = exchange.getBalance(coin);
            double afterKrwBalance = exchange.getBalance(Coin.KRW);
            if (orderType == OrderType.BUY) {
                tradeQuantity = afterCoinBalance - beforeCoinBalance;
            } else { // OrderType.SELL
                tradeQuantity = exchange.getAvailableBuyQuantity(coin, (long) (afterKrwBalance - beforeKrwBalance));
            }
            ArbitrageMarketPrice marketPrice = exchange.getArbitrageMarketPrice(coin, reversedPriceType, tradeQuantity);
            long currentPrice = marketPrice.getMaximinimumPrice();

            // 역거래 성공률을 높이기 위한 조치
            if (coin == Coin.XRP) {
                if (orderType == OrderType.BUY) {
                    currentPrice -= this.reverseDiffXRP; // 더 저렴하게 판매
                } else { // orderType == OrderType.SELL
                    currentPrice += this.reverseDiffXRP; // 더 비싸게 구매
                    tradeQuantity = (afterKrwBalance - beforeKrwBalance) / currentPrice;
                }
            }

            /*
            double loss = (price-currentPrice) * tradeQuantity * (orderType == OrderType.BUY ? 1 : -1);
            if (loss > maxLoss) {
                throw new Exception("예상 손실액이 최대 손실 기준액을 넘었습니다. 역거래를 진행하지 않았습니다. (예상 손실액: " + loss + ")");
            }
            */

            String orderId = exchange.makeOrder(reversedOrderType, coin, currentPrice, tradeQuantity);
            waitOrderCompleted(orderId, reversedOrderType, coin);
        } catch (Exception e) {
            throw new Exception("역거래 실패... " + e);
        }
    }

    private void reverseOrder() throws Exception {
        log("거래 취소가 요청되어 역거래를 진행합니다.");
        tryReverseOrder();
        log("역거래 성공!!!");

        Coin reverseCoin;
        double diff;
        if (orderType == OrderType.BUY) {
            double afterKrwBalance = exchange.getBalance(Coin.KRW);
            diff = afterKrwBalance - beforeKrwBalance;
            reverseCoin = Coin.KRW;
        } else { // OrderType.SELL
            double afterCoinBalance = exchange.getBalance(coin);
            diff = afterCoinBalance - beforeCoinBalance;
            reverseCoin = coin;
        }

        if (diff > 0) {
            String logStr = String.format("다행히 이득이다! %+.0f %s", diff, reverseCoin.name());
            log(logStr);
        } else {
            String logStr = String.format("아쉽게도 손해다... %+.0f %s", diff, reverseCoin.name());
            log(logStr);
        }
    }

    private void cancelTrade() {
        synchronized (sharedResource) {
            switch (getTradeStatus()) {
                case START: // 거래 생성 실패
                    setTradeStatus(TradeStatus.ORDER_CANCELED);
                    oppositeTrade.setIsCancelRequired(true, "상대방의 거래 생성 실패");
                    break;
                case ORDER_MADE: // 거래 성사 실패
                    try {
                        cancelOrder();
                        log("거래 취소 완료");
                        setTradeStatus(TradeStatus.ORDER_CANCELED);
                        oppositeTrade.setIsCancelRequired(true, "상대방의 거래 성사 실패");
                    }
                    catch (Exception e) {
                        log(e.getMessage());
                        try {
                            if (exchange.isOrderExist(orderId, coin, orderType)) {
                                setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                                oppositeTrade.setIsCancelRequired(true, "상대방의 거래 성사 실패");
                            }
                            else {
                                log("거래가 존재하지 않음. 즉, 거래가 성공한 상태임");
                                setTradeStatus(TradeStatus.ORDER_COMPLETED);
                            }
                        } catch (Exception e1) {
                            log(e1.getMessage());
                            setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                            oppositeTrade.setIsCancelRequired(true, "상대방의 거래 성사 실패");
                        }
                    }
                    break;
                case ORDER_COMPLETED:
                    try {
                        //reverseOrder();
                        log("이제 역거래는 진행하지 않음");
                        setTradeStatus(TradeStatus.ORDER_CANCELED);
                    }
                    catch (Exception e) {
                        log(e.getMessage());
                        setTradeStatus(TradeStatus.ORDER_CANCEL_FAILED);
                    }
                    break;
                default:
                    log("nothing to do: " + getTradeStatus().name());
                    break;
            }
        }
        log("완료");
    }

    private void cancelOrder() throws Exception {
        boolean isSuccess = false;
        Exception finalException = null;

        log("거래 취소 시도 시작");
        for (int trial = 0; trial < maxWaitingSec; trial++) {
            try {
                exchange.cancelOrder(orderId, orderType, coin, price, quantity);
                isSuccess = true;
                break;
            } catch (Exception e) {
                //log("\t" + (trial+1) + "번째 시도 실패: " + e);
                finalException = e;
            }
            try {
                Thread.sleep(TRIAL_TIME_INTERVAL);
            }
            catch (InterruptedException e) {}
        }

        if(!isSuccess) {
            throw new Exception("거래 취소 실패 " + ((finalException == null) ? "" : finalException));
        }
    }
    // Trade - end


    // Thread - start
    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        // TradeStatus.START
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        try {
            makeOrder();
        }
        catch (Exception e) {
            log(e.getMessage());
            cancelTrade();
            return;
        }

        // TradeStatus.ORDER_MADE
        if(isCancelRequired()) {
            cancelTrade();
            if (!getTradeStatus().equals(TradeStatus.ORDER_COMPLETED))
                return;
        }
        try {
            waitOrderCompleted(orderId, orderType, coin);
        }
        catch (Exception e) {
            log(e.getMessage());
            cancelTrade();
            if (!getTradeStatus().equals(TradeStatus.ORDER_COMPLETED))
                return;
        }

        // TradeStatus.ORDER_COMPLETED
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        synchronized (sharedResource) {
            while(!isTradeCompleted(oppositeTrade.getTradeStatus())) {
                try {
                    log("상대방 거래가 끝날 때까지 대기");
                    sharedResource.wait();
                    log("대기 상태 풀림");
                }
                catch (Exception e) { }
            }
        }
        if(isCancelRequired()) {
            cancelTrade();
            return;
        }
        log("종료");
    }
    // Thread - end
}
