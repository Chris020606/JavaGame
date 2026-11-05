import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Two-player private host. Simulation never waits for a slow client's socket writer. */
public final class CoopServer implements AutoCloseable {
    public static final int DEFAULT_PORT=27878;
    private final ServerSocket listener=new ServerSocket();
    private final ExecutorService workers=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"outpost-coop-io");t.setDaemon(true);return t;});
    private final ScheduledExecutorService simulation=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"outpost-coop-host");t.setDaemon(true);return t;});
    private final ConcurrentMap<Integer,Peer> peers=new ConcurrentHashMap<>();private final Semaphore slots=new Semaphore(6);
    private final AtomicReference<byte[]> broadcast=new AtomicReference<>();
    private final CoopWorld world;private final Consumer<Properties> save;
    private volatile boolean closed;private volatile String warning="";private int savedChapter=-1;
    private final String code=String.format("%06d",new java.security.SecureRandom().nextInt(1000000)),hostKey=UUID.randomUUID().toString();
    public CoopServer(int port,CoopWorld world,Consumer<Properties> save) throws IOException {
        this.world=world;this.save=save;listener.setReuseAddress(true);listener.bind(new InetSocketAddress(port));workers.submit(this::accept);
        simulation.scheduleAtFixedRate(()->{
            if(closed)return;
            try {
                world.tick(1f/60);CoopState state=world.snapshot();
                if(state.tick()%3==0)broadcast.set(CoopProtocol.snapshot(state));
                if(save!=null && state.phase()==CoopState.Phase.PLAYING && state.chapter()!=savedChapter) {
                    savedChapter=state.chapter();try { save.accept(world.checkpoint()); }catch(RuntimeException failure) { warning="Checkpoint could not be saved. The session is still running."; }
                }
            }catch(Exception failure) { warning="Host simulation stopped: "+failure.getMessage();close(); }
        },0,16666667,TimeUnit.NANOSECONDS);
    }
    public int port() { return listener.getLocalPort(); }public String code() { return code; }public String hostKey() { return hostKey; }public String warning() { return warning; }
    private void accept() {
        while(!closed)try {
            Socket socket=listener.accept();if(!slots.tryAcquire()) { socket.close();continue; }
            workers.submit(()->handshake(socket));
        }catch(IOException stopped) { if(!closed)warning="Connection listener stopped."; }
    }
    private void handshake(Socket socket) {
        Peer peer=null;
        try {
            socket.setTcpNoDelay(true);socket.setSoTimeout(5000);socket.setSendBufferSize(65536);
            DataInputStream in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));DataOutputStream out=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            try {
                int version=in.readInt();int slot=in.readInt();String secret=in.readUTF(),name=in.readUTF();int hero=in.readInt();
                if(version!=CoopProtocol.VERSION)throw new IllegalArgumentException("Different co-op versions. Both players must install the same release.");
                if(slot==0?(!socket.getInetAddress().isLoopbackAddress() || !secret.equals(hostKey)):slot!=1 || !secret.equals(code))throw new IllegalArgumentException("Incorrect session code");
                if(hero<0 || hero>=HeroClass.values().length)throw new IllegalArgumentException("Invalid class");
                synchronized(peers) {
                    if(peers.containsKey(slot))throw new IllegalArgumentException("This two-player session is full");
                    world.attach(slot,name,HeroClass.values()[hero]);peer=new Peer(slot,socket,out);peers.put(slot,peer);
                }
                out.writeUTF("OK");out.writeInt(slot);out.flush();
            }catch(IllegalArgumentException denied) { out.writeUTF(denied.getMessage());out.flush();return; }
            Peer active=peer;workers.submit(()->send(active));
            while(!closed && !socket.isClosed())world.input(peer.slot,CoopProtocol.input(CoopProtocol.frame(in)));
        }catch(IOException disconnected) { /* Keep the hero and pause the world so the same slot can reconnect. */ }
        finally { if(peer!=null)peer.close();try { socket.close(); }catch(IOException ignored) {}slots.release(); }
    }
    private void send(Peer peer) {
        try {
            while(!closed && !peer.socket.isClosed()) { byte[] bytes=broadcast.get();if(bytes!=null)CoopProtocol.write(peer.out,bytes);Thread.sleep(50); }
        }catch(IOException disconnected) { peer.close(); }catch(InterruptedException stopped) { Thread.currentThread().interrupt(); }
    }
    private final class Peer {
        final int slot;final Socket socket;final DataOutputStream out;
        Peer(int slot,Socket socket,DataOutputStream out) { this.slot=slot;this.socket=socket;this.out=out; }
        void close() { if(peers.remove(slot,this))world.detach(slot);try { socket.close(); }catch(IOException ignored) {} }
    }
    @Override public void close() { closed=true;try { listener.close(); }catch(IOException ignored) {}for(Peer p:peers.values())p.close();simulation.shutdownNow();workers.shutdownNow(); }
}
