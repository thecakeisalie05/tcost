package dev.cake.rawcost;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class Settings {
    public Planner.Profile profile=new Planner.Profile();
    public boolean hoverEnabled=false;
    public int quantity=1, hoverLines=8;
    public static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static Settings load(File file) {
        if(!file.isFile()) return new Settings();
        try(Reader r=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)) {
            Settings s=JSON.fromJson(r,Settings.class);
            if(s==null||s.profile==null) throw new IOException("Empty settings");
            s.quantity=Math.max(1,Math.min(1000000,s.quantity));
            s.hoverLines=Math.max(1,Math.min(30,s.hoverLines));
            s.profile.maxDepth=Math.max(1,Math.min(64,s.profile.maxDepth));
            s.profile.maxVisits=Math.max(100,Math.min(50000,s.profile.maxVisits));
            return s;
        } catch(Exception e) {
            TCost.LOG.error("Cannot read TCost settings; defaults loaded without overwriting the file",e);
            return new Settings();
        }
    }
    public void save(File file) throws IOException {
        Files.createDirectories(file.toPath().getParent());
        Path tmp=Files.createTempFile(file.toPath().getParent(),"tcost-",".tmp");
        try {
            Files.write(tmp,JSON.toJson(this).getBytes(StandardCharsets.UTF_8));
            try { Files.move(tmp,file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(tmp,file.toPath(),StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
