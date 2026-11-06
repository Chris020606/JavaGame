import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

class PlayerSavesTest {
    @TempDir Path root;

    private SaveGame.Snapshot snapshot(String name) {
        return new SaveGame.Snapshot(
                name,
                0,
                4,
                0,
                new Progression(),
                new WaveDirector.State(0, 0, 0, 5, 0, 0, 21),
                List.of());
    }

    @Test
    void deletingActivePlayerRemovesItsSaveAndCheckpointButKeepsOthers() throws Exception {
        Path current = root.resolve("save.properties"),
                checkpoint = root.resolve("chapter.properties");
        PlayerSaves saves = new PlayerSaves(root);
        SaveGame.write(current, snapshot("First"));
        SaveGame.write(checkpoint, snapshot("First"));
        saves.newPlayer(current, checkpoint);
        SaveGame.write(current, snapshot("Second"));
        SaveGame.write(checkpoint, snapshot("Second"));
        saves.save(snapshot("Second"), checkpoint);
        saves.save(snapshot("Second"), checkpoint);
        var second =
                saves.entries().stream()
                        .filter(e -> e.snapshot().name().equals("Second"))
                        .findFirst()
                        .orElseThrow();
        saves.delete(second, current, checkpoint);
        assertFalse(Files.exists(current));
        assertFalse(Files.exists(checkpoint));
        assertFalse(Files.exists(root.resolve("players").resolve(second.id() + ".properties.bak")));
        assertEquals(1, new PlayerSaves(root).entries().size());
        var first = saves.entries().getFirst();
        saves.activate(first, current, checkpoint);
        assertEquals("First", SaveGame.read(current).name());
    }

    @Test
    void damagedCurrentSaveIsPreservedAndDoesNotBlockNewPlayer() throws Exception {
        Path current = root.resolve("save.properties");
        Files.writeString(current, "version=invalid");
        PlayerSaves saves = new PlayerSaves(root);
        saves.newPlayer(current, root.resolve("checkpoint"));
        saves.save(snapshot("Fresh"), root.resolve("checkpoint"));
        assertEquals(1, saves.entries().size());
        try (var files = Files.list(root.resolve("players"))) {
            assertTrue(files.anyMatch(p -> p.toString().endsWith(".backup")));
        }
    }

    @Test
    void playersAndCheckpointsRemainIndependentAcrossRestart() throws Exception {
        Path current = root.resolve("save.properties"),
                checkpoint = root.resolve("chapter.properties");
        PlayerSaves saves = new PlayerSaves(root);
        SaveGame.write(current, snapshot("First"));
        SaveGame.write(checkpoint, snapshot("First"));
        saves.newPlayer(current, checkpoint);
        SaveGame.write(checkpoint, snapshot("Second"));
        saves.save(snapshot("Second"), checkpoint);
        saves = new PlayerSaves(root);
        var entries = saves.entries();
        assertEquals(2, entries.size());
        var first =
                entries.stream()
                        .filter(e -> e.snapshot().name().equals("First"))
                        .findFirst()
                        .orElseThrow();
        saves.activate(first, current, checkpoint);
        assertEquals("First", SaveGame.read(current).name());
        assertEquals("First", SaveGame.read(checkpoint).name());
        saves.save(snapshot("First"), checkpoint);
        assertEquals(2, saves.entries().size());
    }
}
