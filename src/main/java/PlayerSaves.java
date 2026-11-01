import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Named campaigns retain independent saves and chapter checkpoints. */
public final class PlayerSaves {
    public record Entry(String id,SaveGame.Snapshot snapshot,long updated) { }
    private final Path root;
    private String active;
    public PlayerSaves(Path directory) { root=directory.resolve("players"); }
    public void newPlayer(Path current,Path checkpoint) throws IOException {
        if(Files.exists(current)) {
            SaveGame.Snapshot previous=null;
            try { previous=SaveGame.read(current); }
            catch(IOException damaged) { Files.createDirectories(root);Files.copy(current,root.resolve("unreadable-"+UUID.randomUUID()+".backup")); }
            if(previous!=null)save(previous,checkpoint);
        }
        active=UUID.randomUUID().toString();
    }
    private String active() throws IOException {
        if(active==null) {
            Path marker=root.resolve("active.properties");
            active=Files.exists(marker)?AppData.read(marker).getProperty("id"):UUID.randomUUID().toString();
            if(active==null || !active.matches("[a-zA-Z0-9-]+"))active=UUID.randomUUID().toString();
        }
        return active;
    }
    public void save(SaveGame.Snapshot snapshot,Path checkpoint) throws IOException {
        String id=active();SaveGame.write(root.resolve(id+".properties"),snapshot);
        if(Files.exists(checkpoint))Files.copy(checkpoint,root.resolve(id+".checkpoint"),StandardCopyOption.REPLACE_EXISTING);
        Properties marker=new Properties();marker.setProperty("id",id);AppData.write(root.resolve("active.properties"),marker);
    }
    public List<Entry> entries() throws IOException {
        if(!Files.isDirectory(root))return List.of();
        List<Entry> result=new ArrayList<>();
        try(var files=Files.list(root)) {
            for(Path file:files.filter(p->p.getFileName().toString().endsWith(".properties") && !p.getFileName().toString().equals("active.properties")).toList()) {
                try { String name=file.getFileName().toString();result.add(new Entry(name.substring(0,name.length()-11),SaveGame.read(file),Files.getLastModifiedTime(file).toMillis())); }
                catch(IOException ignored) { /* One damaged save must not hide other players. */ }
            }
        }
        result.sort(Comparator.comparingLong(Entry::updated).reversed());return List.copyOf(result);
    }
    public void delete(Entry entry,Path current,Path checkpoint) throws IOException {
        if(!entry.id().matches("[a-zA-Z0-9-]+"))throw new IOException("Invalid player identifier");
        if(entry.id().equals(active())) {
            for(Path path:List.of(current,checkpoint,current.resolveSibling(current.getFileName()+".bak"),checkpoint.resolveSibling(checkpoint.getFileName()+".bak"),root.resolve("active.properties"),root.resolve("active.properties.bak")))Files.deleteIfExists(path);
            active=null;
        }
        for(String suffix:List.of(".properties",".properties.bak",".checkpoint"))Files.deleteIfExists(root.resolve(entry.id()+suffix));
    }
    public void activate(Entry entry,Path current,Path checkpoint) throws IOException {
        SaveGame.Snapshot snapshot=SaveGame.read(root.resolve(entry.id+".properties"));
        SaveGame.write(current,snapshot);Path source=root.resolve(entry.id+".checkpoint");
        if(Files.exists(source))Files.copy(source,checkpoint,StandardCopyOption.REPLACE_EXISTING);else Files.deleteIfExists(checkpoint);
        active=entry.id;Properties marker=new Properties();marker.setProperty("id",active);AppData.write(root.resolve("active.properties"),marker);
    }
}
