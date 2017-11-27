package pe.joyyir.Heungbubak.Routine;

import pe.joyyir.Heungbubak.Exchange.Service.BittrexService;

import java.util.Arrays;
import java.util.List;

public class CandidateCoinRoutine implements Routine {
    @Override
    public void run() {
        System.out.println("Condidiate Coins : " + getCondidiateCoinList().toString());
    }

    private List<String> getCondidiateCoinList() {
        BittrexService service = new BittrexService();

        // 후보에서 제외할 코인
        final String[] exclude = {
                "2GIVE", "SNRG", "XMG", "BYC", "EGC",
                "GLD", "SEQ", "MLN", "INFX", "EFL", // 상승장
                "APX", "AUR" // 상승 상태에서 횡보
        };

        // 소유하고 있는 코인
        final String[] own = {
                "BTC", "DNT", "VIB", "XEL", "EXP",
                "MTL", "ADT", "NLG", "DGB", "BLITZ",
                "MCO", "CPC", "CRB", "ZCL", "GEO",
                "1ST", "SNGLS"};

        return service.getCandidateCoinList("BTC", Arrays.asList(exclude), Arrays.asList(own));
    }

    public static void main(String[] args) {
        new CandidateCoinRoutine().run();
    }
}
