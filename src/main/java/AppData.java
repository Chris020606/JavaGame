import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** User files stay outside the install directory. Atomic replacement protects existing saves. */
public final class AppData {
    private AppData() {}
    public static Path directory() {
        String override = System.getProperty("outpost.dataDir");

        if (override!=null)
            return Path.of(override);
        String os = System.getProperty("os.name").toLowerCase(),home=System.getProperty("user.home");

        if (os.contains("win")) {
            String appData=System.getenv("APPDATA");
            return appData==null?Path.of(home,"Outpost"):Path.of(appData,"Outpost");
        }

        if (os.contains("mac"))
            return Path.of(home,"Library","Application Support","Outpost");

        String xdg=System.getenv("XDG_DATA_HOME");
        return xdg!=null&&!xdg.isBlank()?Path.of(xdg,"outpost"):Path.of(home,".local","share","outpost");
    }
    public static Properties read(Path file) throws IOException {
        if (Files.size(file)>1024*1024)
            throw new IOException("File is too large");
        Properties p=new Properties();
        try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
        catch (IllegalArgumentException e)
            { throw new IOException("Invalid file format",e); }
        return p;
    }
    public static void write(Path file,Properties p) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp=Files.createTempFile(file.toAbsolutePath().getParent(),"outpost-",".tmp");
        try {
            try (Writer w=Files.newBufferedWriter(temp)) { p.store(w,"Outpost user data"); }
            if (Files.exists(file)) Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
