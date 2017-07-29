package pe.joyyir.Heungbubak.Exchange.Service;

import org.junit.Test;

import static org.junit.Assert.*;

public class BithumbServiceTest {
    @Test
    public void getCompleteBalance() throws Exception {
        BithumbService service = new BithumbService();
        System.out.println(service.getCompleteBalance());
    }

}