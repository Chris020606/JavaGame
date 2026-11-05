import Entities.Difficulty;
import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class CoopNetworkTest {
    private static void await(java.util.function.BooleanSupplier condition) throws Exception { long deadline=System.nanoTime()+4_000_000_000L;while(!condition.getAsBoolean() && System.nanoTime()<deadline)Thread.sleep(10);assertTrue(condition.getAsBoolean(),"Timed out waiting for network state"); }
    @Test void boundedCodecRoundTripsAndRejectsMalformedPackets() throws Exception {
        CoopWorld w=CoopWorldTest.started();for(int tick=0;tick<300;tick++) { w.input(0,new CoopInput(tick+2,0,0,0,CoopInput.LIGHT));w.input(1,new CoopInput(tick+2,0,0,0,CoopInput.LIGHT));w.tick(1f/60); }CoopState state=w.snapshot();assertFalse(state.enemies().isEmpty());assertEquals(state,CoopProtocol.snapshot(CoopProtocol.snapshot(state)));
        CoopInput i=new CoopInput(7,.5f,-1,45,CoopInput.LIGHT);assertEquals(i,CoopProtocol.input(CoopProtocol.input(i)));
        assertThrows(IOException.class,()->CoopProtocol.frame(new DataInputStream(new ByteArrayInputStream(new byte[]{0,2,0,0}))));
        assertThrows(IOException.class,()->CoopProtocol.snapshot(new byte[]{0,0}));
    }
    @Test void twoRealClientsShareMovementPauseReconnectAndRejectThirdPlayers() throws Exception {
        try(CoopServer server=new CoopServer(0,new CoopWorld(Difficulty.NORMAL,4),null);
            CoopClient host=new CoopClient("127.0.0.1",server.port(),0,server.hostKey(),"Host",HeroClass.WARRIOR);
            CoopClient guest=new CoopClient("127.0.0.1",server.port(),1,server.code(),"Guest",HeroClass.ARCHER)) {
            await(()->host.state()!=null && host.state().heroes().size()==2 && guest.state()!=null);
            assertThrows(IOException.class,()->new CoopClient("127.0.0.1",server.port(),1,"000000x","Bad",HeroClass.MAGE));
            assertThrows(IOException.class,()->new CoopClient("127.0.0.1",server.port(),1,server.code(),"Third",HeroClass.MAGE));
            host.submit(new CoopInput(1,0,0,0,CoopInput.READY));host.submit(new CoopInput(2,0,0,0,0));guest.submit(new CoopInput(1,0,0,0,CoopInput.READY));guest.submit(new CoopInput(2,0,0,0,0));
            await(()->host.state().phase()==CoopState.Phase.PLAYING && guest.state().phase()==CoopState.Phase.PLAYING);
            float start=host.state().heroes().get(0).x();host.submit(new CoopInput(3,1,0,90,0));guest.submit(new CoopInput(3,0,0,0,0));
            await(()->guest.state().heroes().get(0).x()>start+.3f);
            guest.close();await(()->host.state().paused() && !host.state().heroes().get(1).connected());
            try(CoopClient reconnect=new CoopClient("127.0.0.1",server.port(),1,server.code(),"Guest",HeroClass.ARCHER)) {
                host.submit(new CoopInput(4,0,0,0,0));reconnect.submit(new CoopInput(1,0,0,0,0));await(()->host.state().heroes().get(1).connected() && !host.state().paused());
            }
        }
    }
}
