import static org.junit.jupiter.api.Assertions.*;

import Entities.Difficulty;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

/** This binary was captured from the published v4.2.1 implementation before refactoring. */
class ProtocolCompatibilityTest {
    @Test
    void populatedPublishedPacketPreservesAllRecordTypes() throws IOException {
        byte[] expected;
        try (var stream = getClass().getResourceAsStream("/coop-protocol-v1-active.bin")) {
            assertNotNull(stream);
            expected = stream.readAllBytes();
        }
        var state = CoopProtocol.snapshot(expected);
        assertEquals(321, state.tick());
        assertEquals(CoopState.Phase.PLAYING, state.phase());
        assertEquals(2, state.heroes().size());
        assertEquals("Fixture", state.heroes().get(0).name());
        assertTrue(state.heroes().get(0).heavy());
        assertEquals(5, state.enemies().get(0).variant());
        assertEquals(3, state.enemies().get(0).move());
        assertTrue(state.bolts().get(0).magic());
        assertEquals(1, state.dangers().get(0).kind());
        assertArrayEquals(expected, CoopProtocol.snapshot(state));
    }

    @Test
    void publishedVersionOneSnapshotStillDecodesAndEncodesIdentically() throws IOException {
        byte[] expected;
        try (var stream = getClass().getResourceAsStream("/coop-protocol-v1.bin")) {
            assertNotNull(stream);
            expected = stream.readAllBytes();
        }
        var state = CoopProtocol.snapshot(expected);
        assertEquals(CoopState.Phase.LOBBY, state.phase());
        assertEquals("One", state.heroes().get(0).name());
        assertEquals("Two", state.heroes().get(1).name());
        assertEquals(100, state.heroes().get(0).health());
        assertEquals(80, state.heroes().get(1).health());
        assertArrayEquals(expected, CoopProtocol.snapshot(state));
        var world = new CoopWorld(Difficulty.NORMAL, 42);
        world.attach(0, "One", HeroClass.WARRIOR);
        world.attach(1, "Two", HeroClass.MAGE);
        assertArrayEquals(expected, CoopProtocol.snapshot(world.snapshot()));
        byte[] trailing = Arrays.copyOf(expected, expected.length + 1);
        assertThrows(IOException.class, () -> CoopProtocol.snapshot(trailing));
    }
}
