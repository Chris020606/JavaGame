import java.io.*;
import java.util.*;

/** Bounded binary messages; deliberately avoids Java object deserialization over the network. */
public final class CoopProtocol {
    public static final int VERSION = 1, MAX_FRAME = 65536;

    public static byte[] frame(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 1 || n > MAX_FRAME) throw new IOException("Invalid co-op packet size");
        byte[] b = new byte[n];
        in.readFully(b);
        return b;
    }

    public static void write(DataOutputStream out, byte[] bytes) throws IOException {
        if (bytes.length > MAX_FRAME) throw new IOException("Co-op snapshot is too large");
        out.writeInt(bytes.length);
        out.write(bytes);
        out.flush();
    }

    public static byte[] input(CoopInput i) throws IOException {
        var b = new ByteArrayOutputStream();
        var d = new DataOutputStream(b);
        d.writeLong(i.sequence());
        d.writeFloat(i.moveX());
        d.writeFloat(i.moveZ());
        d.writeFloat(i.aim());
        d.writeInt(i.buttons());
        return b.toByteArray();
    }

    public static CoopInput input(byte[] b) throws IOException {
        try {
            var d = new DataInputStream(new ByteArrayInputStream(b));
            CoopInput i =
                    new CoopInput(
                            d.readLong(), d.readFloat(), d.readFloat(), d.readFloat(), d.readInt());
            if (d.available() != 0) throw new IOException("Extra input data");
            return i;
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid co-op input", invalid);
        }
    }

    private static void writeFloats(DataOutputStream d, float... values) throws IOException {
        for (float f : values) d.writeFloat(f);
    }

    private static float readFiniteFloat(DataInputStream d) throws IOException {
        float f = d.readFloat();
        if (!Float.isFinite(f)) throw new IOException("Invalid snapshot number");
        return f;
    }

    private static int readBoundedCount(DataInputStream d, int max) throws IOException {
        int n = d.readInt();
        if (n < 0 || n > max) throw new IOException("Invalid snapshot count");
        return n;
    }

    private static int readOrdinal(DataInputStream d, int max) throws IOException {
        int n = d.readInt();
        if (n < 0 || n >= max) throw new IOException("Invalid snapshot value");
        return n;
    }

    public static byte[] snapshot(CoopState s) throws IOException {
        var b = new ByteArrayOutputStream();
        var d = new DataOutputStream(b);
        d.writeLong(s.tick());
        d.writeInt(s.phase().ordinal());
        d.writeInt(s.chapter());
        d.writeInt(s.wave());
        d.writeInt(s.queued());
        writeFloats(d, s.rest(), s.baseHealth(), s.baseMax());
        d.writeInt(s.kills());
        d.writeInt(s.difficulty());
        d.writeBoolean(s.paused());
        d.writeInt(s.heroes().size());
        for (var h : s.heroes()) {
            d.writeInt(h.slot());
            d.writeUTF(h.name());
            d.writeInt(h.hero());
            d.writeBoolean(h.connected());
            d.writeBoolean(h.ready());
            d.writeBoolean(h.paused());
            writeFloats(
                    d,
                    h.x(),
                    h.z(),
                    h.facing(),
                    h.health(),
                    h.maxHealth(),
                    h.stamina(),
                    h.maxStamina(),
                    h.vx(),
                    h.vz(),
                    h.attack(),
                    h.duration(),
                    h.impact());
            d.writeBoolean(h.heavy());
            d.writeInt(h.combo());
            writeFloats(d, h.charge(), h.dodge(), h.parry(), h.hurt(), h.revive(), h.repair());
            d.writeInt(h.potions());
            d.writeInt(h.credits());
            d.writeInt(h.vitality());
            d.writeInt(h.power());
            d.writeInt(h.endurance());
        }
        d.writeInt(s.enemies().size());
        for (var e : s.enemies()) {
            d.writeInt(e.id());
            d.writeInt(e.wave());
            d.writeInt(e.variant());
            writeFloats(
                    d,
                    e.personality(),
                    e.x(),
                    e.z(),
                    e.facing(),
                    e.health(),
                    e.maxHealth(),
                    e.attack(),
                    e.duration(),
                    e.attackX(),
                    e.attackZ());
            d.writeInt(e.move());
            writeFloats(d, e.charge(), e.stun(), e.clock(), e.death());
        }
        d.writeInt(s.bolts().size());
        for (var p : s.bolts()) {
            d.writeInt(p.id());
            writeFloats(d, p.x(), p.z(), p.vx(), p.vz());
            d.writeBoolean(p.magic());
            d.writeBoolean(p.hostile());
        }
        d.writeInt(s.dangers().size());
        for (var h : s.dangers()) {
            d.writeInt(h.id());
            d.writeInt(h.kind());
            writeFloats(d, h.x(), h.z(), h.delay(), h.radius(), h.age());
        }
        return b.toByteArray();
    }

    public static CoopState snapshot(byte[] bytes) throws IOException {
        var d = new DataInputStream(new ByteArrayInputStream(bytes));
        long tick = d.readLong();
        var phase = CoopState.Phase.values()[readOrdinal(d, 5)];
        int chapter = readOrdinal(d, 4), wave = d.readInt(), queued = d.readInt();
        float rest = readFiniteFloat(d), hp = readFiniteFloat(d), max = readFiniteFloat(d);
        int kills = d.readInt(), difficulty = readOrdinal(d, 4);
        boolean paused = d.readBoolean();
        List<CoopState.Hero> heroes = new ArrayList<>();
        int heroCount = readBoundedCount(d, 2);
        for (int i = 0; i < heroCount; i++) heroes.add(readHero(d));
        List<CoopState.Mob> enemies = new ArrayList<>();
        int enemyCount = readBoundedCount(d, 128);
        for (int i = 0; i < enemyCount; i++) enemies.add(readMob(d));
        List<CoopState.Bolt> bolts = new ArrayList<>();
        int boltCount = readBoundedCount(d, 256);
        for (int i = 0; i < boltCount; i++) bolts.add(readBolt(d));
        List<CoopState.Danger> dangers = new ArrayList<>();
        int dangerCount = readBoundedCount(d, 64);
        for (int i = 0; i < dangerCount; i++) dangers.add(readDanger(d));
        if (d.available() != 0) throw new IOException("Extra snapshot data");
        return new CoopState(
                tick,
                phase,
                chapter,
                wave,
                queued,
                rest,
                hp,
                max,
                kills,
                difficulty,
                paused,
                List.copyOf(heroes),
                List.copyOf(enemies),
                List.copyOf(bolts),
                List.copyOf(dangers));
    }

    /** Reads hero fields in the protocol-v1 wire order. */
    private static CoopState.Hero readHero(DataInputStream input) throws IOException {
        int slot = readOrdinal(input, 2);
        String name = input.readUTF();
        if (name.length() > 24) throw new IOException("Invalid hero name");
        int hero = readOrdinal(input, 3);
        boolean connected = input.readBoolean();
        boolean ready = input.readBoolean();
        boolean paused = input.readBoolean();
        float x = readFiniteFloat(input);
        float z = readFiniteFloat(input);
        float facing = readFiniteFloat(input);
        float health = readFiniteFloat(input);
        float maxHealth = readFiniteFloat(input);
        float stamina = readFiniteFloat(input);
        float maxStamina = readFiniteFloat(input);
        float vx = readFiniteFloat(input);
        float vz = readFiniteFloat(input);
        float attack = readFiniteFloat(input);
        float duration = readFiniteFloat(input);
        float impact = readFiniteFloat(input);
        boolean heavy = input.readBoolean();
        int combo = input.readInt();
        float charge = readFiniteFloat(input);
        float dodge = readFiniteFloat(input);
        float parry = readFiniteFloat(input);
        float hurt = readFiniteFloat(input);
        float revive = readFiniteFloat(input);
        float repair = readFiniteFloat(input);
        int potions = input.readInt();
        int credits = input.readInt();
        int vitality = input.readInt();
        int power = input.readInt();
        int endurance = input.readInt();
        return new CoopState.Hero(
                slot,
                name,
                hero,
                connected,
                ready,
                paused,
                x,
                z,
                facing,
                health,
                maxHealth,
                stamina,
                maxStamina,
                vx,
                vz,
                attack,
                duration,
                impact,
                heavy,
                combo,
                charge,
                dodge,
                parry,
                hurt,
                revive,
                repair,
                potions,
                credits,
                vitality,
                power,
                endurance);
    }

    /** Reads mob fields in the protocol-v1 wire order. */
    private static CoopState.Mob readMob(DataInputStream input) throws IOException {
        int id = input.readInt();
        int wave = input.readInt();
        int variant = readOrdinal(input, 6);
        float personality = readFiniteFloat(input);
        float x = readFiniteFloat(input);
        float z = readFiniteFloat(input);
        float facing = readFiniteFloat(input);
        float health = readFiniteFloat(input);
        float maxHealth = readFiniteFloat(input);
        float attack = readFiniteFloat(input);
        float duration = readFiniteFloat(input);
        float attackX = readFiniteFloat(input);
        float attackZ = readFiniteFloat(input);
        int move = readOrdinal(input, 4);
        float charge = readFiniteFloat(input);
        float stun = readFiniteFloat(input);
        float clock = readFiniteFloat(input);
        float death = readFiniteFloat(input);
        return new CoopState.Mob(
                id,
                wave,
                variant,
                personality,
                x,
                z,
                facing,
                health,
                maxHealth,
                attack,
                duration,
                attackX,
                attackZ,
                move,
                charge,
                stun,
                clock,
                death);
    }

    /** Reads bolt fields in the protocol-v1 wire order. */
    private static CoopState.Bolt readBolt(DataInputStream input) throws IOException {
        int id = input.readInt();
        float x = readFiniteFloat(input);
        float z = readFiniteFloat(input);
        float vx = readFiniteFloat(input);
        float vz = readFiniteFloat(input);
        boolean magic = input.readBoolean();
        boolean hostile = input.readBoolean();
        return new CoopState.Bolt(id, x, z, vx, vz, magic, hostile);
    }

    /** Reads danger fields in the protocol-v1 wire order. */
    private static CoopState.Danger readDanger(DataInputStream input) throws IOException {
        int id = input.readInt();
        int kind = readOrdinal(input, 2);
        float x = readFiniteFloat(input);
        float z = readFiniteFloat(input);
        float delay = readFiniteFloat(input);
        float radius = readFiniteFloat(input);
        float age = readFiniteFloat(input);
        return new CoopState.Danger(id, kind, x, z, delay, radius, age);
    }

    private CoopProtocol() {}
}
