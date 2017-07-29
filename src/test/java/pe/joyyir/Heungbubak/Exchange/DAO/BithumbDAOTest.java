package pe.joyyir.Heungbubak.Exchange.DAO;

import org.junit.Test;
import pe.joyyir.Heungbubak.Exchange.Domain.CoinPriceVO;

import static org.junit.Assert.*;

public class BithumbDAOTest {
    @Test
    public void getCoinPriceVO() throws Exception {
        BithumbDAO dao = new BithumbDAO();
        CoinPriceVO vo = dao.getCoinPriceVO();
        System.out.println(vo);
    }

}