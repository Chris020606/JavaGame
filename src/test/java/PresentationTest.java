import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;
import static org.junit.jupiter.api.Assertions.*;

class PresentationTest {
    @Test void authoredLayoutsStayRepeatableAndKeepCombatCenterClear() {
        Set<String> signatures=new HashSet<>();
        for(int chapter=0;chapter<4;chapter++) {
            var props=EnvironmentLayout.props(chapter);assertSame(props,EnvironmentLayout.props(chapter));
            assertTrue(props.size()>60);signatures.add(props.toString());
            for(var p:props) {
                assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.z()) && Float.isFinite(p.yaw()));assertTrue(p.scale()>0 && p.scale()<12);
                assertTrue(Math.hypot(p.x(),p.z())>6.5,"Scenery intrudes into sanctuary: "+p);
                String folder=p.graveyard()?"graveyard":"nature";
                assertTrue(Files.exists(Path.of("resources/outpost",folder,p.model()+".obj")),p.model());
            }
            var path=EnvironmentLayout.path(chapter);assertTrue(path.get(0).z()<=-30);assertTrue(path.get(path.size()-1).z()>=30);
            assertTrue(path.stream().anyMatch(p->p.x()==0 && p.z()==0));
        }
        assertEquals(4,signatures.size());
    }
    @Test void musicIsStereoLoopableAndNotClippedOrSilent() throws Exception {
        for(String theme:List.of("title","meadow","cemetery","frost","necropolis","battle")) {
            try(AudioInputStream stream=AudioSystem.getAudioInputStream(Path.of("resources/audio/music-"+theme+".wav").toFile())) {
                assertEquals(2,stream.getFormat().getChannels());assertEquals(24000,stream.getFormat().getSampleRate());assertEquals(24*24000,stream.getFrameLength());
                byte[] pcm=stream.readAllBytes();double energy=0,stereo=0;int peak=0;
                for(int i=0;i<pcm.length;i+=4) {
                    int left=(short)((pcm[i]&255)|(pcm[i+1]<<8)),right=(short)((pcm[i+2]&255)|(pcm[i+3]<<8));
                    peak=Math.max(peak,Math.abs(left));energy+=(double)left*left;stereo+=Math.abs(left-right);
                }
                assertTrue(peak<31000,"Clipped "+theme);assertTrue(Math.sqrt(energy/stream.getFrameLength())>100,"Silent "+theme);assertTrue(stereo/stream.getFrameLength()>10,"Mono "+theme);
                // A seamless loop must not introduce a discontinuity louder than a transient.
                for(int channel=0;channel<2;channel++) {
                    int first=(short)((pcm[channel*2]&255)|(pcm[channel*2+1]<<8));int last=pcm.length-4+channel*2;
                    int end=(short)((pcm[last]&255)|(pcm[last+1]<<8));assertTrue(Math.abs(first-end)<1500,"Loop click in "+theme);
                }
            }
        }
    }
    @Test void originalFoleyFilesAreStereoFiniteAndShort() throws Exception {
        for(String name:List.of("swing0","swing1","swing2","hit0","hit1","hit2","parry","bow","magic","ward","slam","upgrade"))
            try(AudioInputStream stream=AudioSystem.getAudioInputStream(Path.of("resources/audio/"+name+".wav").toFile())) {
                assertEquals(2,stream.getFormat().getChannels());assertTrue(stream.getFrameLength()>4000 && stream.getFrameLength()<90000);
            }
    }
}
