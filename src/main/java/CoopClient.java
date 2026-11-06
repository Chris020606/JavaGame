import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** UI submits intentions without blocking on network I/O. Readers publish immutable snapshots. */
public final class CoopClient implements AutoCloseable {
    private final Socket socket = new Socket();
    private final BlockingQueue<CoopInput> commands = new ArrayBlockingQueue<>(128);
    private final ExecutorService workers =
            Executors.newFixedThreadPool(
                    2,
                    r -> {
                        Thread t = new Thread(r, "outpost-coop-client");
                        t.setDaemon(true);
                        return t;
                    });
    private volatile CoopState state;
    private volatile String error = "";
    private volatile boolean closed;
    private volatile long receivedAt;
    private final int slot;

    public CoopClient(
            String address, int port, int requestedSlot, String code, String name, HeroClass hero)
            throws IOException {
        try {
            socket.connect(new InetSocketAddress(address, port), 3500);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(6000);
            var out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            var in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out.writeInt(CoopProtocol.VERSION);
            out.writeInt(requestedSlot);
            out.writeUTF(code);
            out.writeUTF(name);
            out.writeInt(hero.ordinal());
            out.flush();
            String response = in.readUTF();
            if (!response.equals("OK")) throw new IOException(response);
            slot = in.readInt();
            workers.submit(() -> {
                try {
                    while (!closed) {
                        state = CoopProtocol.snapshot(CoopProtocol.frame(in));
                        receivedAt = System.nanoTime();
                    }
                } catch (IOException failure) {
                    failed("Connection lost. Reconnect to resume the paused session.");
                }
            });
            workers.submit(() -> {
                try {
                    CoopInput last = CoopInput.NONE;
                    while (!closed) {
                        CoopInput next = commands.poll(33, TimeUnit.MILLISECONDS);
                        if (next != null) last = next;
                        CoopProtocol.write(out, CoopProtocol.input(last));
                    }
                } catch (IOException failure) {
                    failed("Connection lost. Reconnect to resume the paused session.");
                } catch (InterruptedException stopped) {
                    Thread.currentThread().interrupt();
                }
            });
        } catch (IOException | RuntimeException failed) {
            close();
            throw failed;
        }
    }

    public void submit(CoopInput command) {
        if (!closed && !commands.offer(command))
            failed("Network is too slow. Reconnect to resume safely.");
    }

    public CoopState state() {
        return state;
    }

    public int slot() {
        return slot;
    }

    public String error() {
        return error;
    }

    public float snapshotAge() {
        return receivedAt == 0
                ? 0
                : Math.min(.1f, (System.nanoTime() - receivedAt) / 1_000_000_000f);
    }

    private void failed(String message) {
        if (!closed) {
            error = message;
            close();
        }
    }

    @Override
    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        workers.shutdownNow();
    }
}
