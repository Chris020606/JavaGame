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

    private static void floats(DataOutputStream d, float... values) throws IOException {
        for (float f : values) d.writeFloat(f);
    }

    private static float f(DataInputStream d) throws IOException {
        float f = d.readFloat();
        if (!Float.isFinite(f)) throw new IOException("Invalid snapshot number");
        return f;
    }

    private static int count(DataInputStream d, int max) throws IOException {
        int n = d.readInt();
        if (n < 0 || n > max) throw new IOException("Invalid snapshot count");
        return n;
    }

    private static int ordinal(DataInputStream d, int max) throws IOException {
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
        floats(d, s.rest(), s.baseHealth(), s.baseMax());
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
            floats(
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
            floats(d, h.charge(), h.dodge(), h.parry(), h.hurt(), h.revive(), h.repair());
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
            floats(
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
            floats(d, e.charge(), e.stun(), e.clock(), e.death());
        }
        d.writeInt(s.bolts().size());
        for (var p : s.bolts()) {
            d.writeInt(p.id());
            floats(d, p.x(), p.z(), p.vx(), p.vz());
            d.writeBoolean(p.magic());
            d.writeBoolean(p.hostile());
        }
        d.writeInt(s.dangers().size());
        for (var h : s.dangers()) {
            d.writeInt(h.id());
            d.writeInt(h.kind());
            floats(d, h.x(), h.z(), h.delay(), h.radius(), h.age());
        }
        return b.toByteArray();
    }

    public static CoopState snapshot(byte[] bytes) throws IOException {
        var d = new DataInputStream(new ByteArrayInputStream(bytes));
        long tick = d.readLong();
        var phase = CoopState.Phase.values()[ordinal(d, 5)];
        int chapter = ordinal(d, 4), wave = d.readInt(), queued = d.readInt();
        float rest = f(d), hp = f(d), max = f(d);
        int kills = d.readInt(), difficulty = ordinal(d, 4);
        boolean paused = d.readBoolean();
        List<CoopState.Hero> heroes = new ArrayList<>();
        int n = count(d, 2);
        for (int i = 0; i < n; i++) {
            int slot = ordinal(d, 2);
            String name = d.readUTF();
            if (name.length() > 24) throw new IOException("Invalid hero name");
            int hero = ordinal(d, 3);
            boolean online = d.readBoolean(), ready = d.readBoolean(), pause = d.readBoolean();
            heroes.add(
                    new CoopState.Hero(
                            slot,
                            name,
                            hero,
                            online,
                            ready,
                            pause,
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            d.readBoolean(),
                            d.readInt(),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            d.readInt(),
                            d.readInt(),
                            d.readInt(),
                            d.readInt(),
                            d.readInt()));
        }
        List<CoopState.Mob> enemies = new ArrayList<>();
        n = count(d, 128);
        for (int i = 0; i < n; i++)
            enemies.add(
                    new CoopState.Mob(
                            d.readInt(),
                            d.readInt(),
                            ordinal(d, 6),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            f(d),
                            ordinal(d, 4),
                            f(d),
                            f(d),
                            f(d),
                            f(d)));
        List<CoopState.Bolt> bolts = new ArrayList<>();
        n = count(d, 256);
        for (int i = 0; i < n; i++)
            bolts.add(
                    new CoopState.Bolt(
                            d.readInt(), f(d), f(d), f(d), f(d), d.readBoolean(), d.readBoolean()));
        List<CoopState.Danger> dangers = new ArrayList<>();
        n = count(d, 64);
        for (int i = 0; i < n; i++)
            dangers.add(
                    new CoopState.Danger(d.readInt(), ordinal(d, 2), f(d), f(d), f(d), f(d), f(d)));
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

    private CoopProtocol() {}
}
