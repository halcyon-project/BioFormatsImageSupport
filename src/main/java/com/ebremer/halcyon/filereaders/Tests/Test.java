package com.ebremer.halcyon.filereaders.Tests;

import com.ebremer.capella.bioformats.FileRandomAccess;
import com.ebremer.capella.bioformats.HTTPRandomAccess;
import com.ebremer.halcyon.lib.URITools;
import is.halcyon.storage.Tools;
import is.halcyon.storage.URIObject;
import is.halcyon.storage.providers.File.FileLWS;
import is.halcyon.storage.providers.S3.S3LWS;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import loci.common.Location;
import loci.formats.FormatException;
import loci.formats.in.SVSReader;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Erich Bremer
 */
public class Test {
    
    public static void main(String[] args) throws FormatException, IOException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.OFF);   
        String f = "D:\\HalcyonStorage\\tcga\\brca\\TCGA-3C-AAAU-01A-01-TS1.2F52DD63-7476-4E85-B7C6-E06092DB6CC1.svs";
        File ff = new File(f);
        
        
        //URI uri = ff.toURI();
        //System.out.println(URITools.fix(ff.toURI()));
        //HTTPRandomAccess bbb = new HTTPRandomAccess(URITools.fix(ff.toURI()));
        //Location.mapFile("charm", bbb);
       
        URI target = URI.create("https://ebremer.com/HalcyonStorage/capella/image.svs");
        
        URI back = Tools.normalize(Path.of("/HalcyonStorage").toUri());
        URI front = URI.create("https://ebremer.com/HalcyonStorage/");
        System.out.println("FB : "+front+" "+back);
        
        long start = System.nanoTime();
        FileLWS.FileLWSBuilder builder = new FileLWS.FileLWSBuilder();
        FileLWS lws = (FileLWS) builder
            .setFrontURI(front)
            .setBackURI(back)
            .build();
        
        URIObject uri = lws.getURIObject(target);
        //uri.getMeta().getModel().write(System.out, "TTL");
        SeekableByteChannel sbc = uri.getSeekableByteChannel();
        FileRandomAccess fra = new FileRandomAccess(sbc);
        Location.mapFile("charm", fra);
        SVSReader r = new SVSReader();
        r.setId("charm");
        r.setSeries(0);
        System.out.println(r.getSizeX()+" x "+r.getSizeY());
        long second = System.nanoTime();
        
        URI neo = URI.create("https://ebremer.com/HalcyonStorage/image.svs");
        URI back2 = URI.create("s3:///ebremeribox/");
        URI front2 = URI.create("https://ebremer.com/HalcyonStorage/");
        System.out.println("FB : "+front2+" "+back2);
        S3LWS.S3LWSBuilder builder2 = new S3LWS.S3LWSBuilder();
        S3LWS lws2 = (S3LWS) builder2
            .setFrontURI(front2)
            .setBackURI(back2)
            .build();
        
        URIObject uri2 = lws2.getURIObject(neo);
        
        try (SeekableByteChannel sbc2 = uri2.getSeekableByteChannel()) {
            FileRandomAccess fra2 = new FileRandomAccess(sbc2);
            Location.mapFile("charm2", fra2);
            SVSReader r2 = new SVSReader();
            r2.setId("charm2");
            r2.setSeries(0);
            System.out.println(r2.getSizeX()+" x "+r2.getSizeY());
            r2.close();
            fra2.close();
        }
        lws2.close();
        
        long end = System.nanoTime();
        
        System.out.println((second-start)/1000000000f);
        System.out.println((end-second) /1000000000f);
        
    }
    
}
