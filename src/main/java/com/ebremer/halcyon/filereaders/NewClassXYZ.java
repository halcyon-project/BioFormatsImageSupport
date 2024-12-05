package com.ebremer.halcyon.filereaders;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import loci.formats.FormatException;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.ZarrReader;


/**
 *
 * @author Erich Bremer
 */
public class NewClassXYZ {
    
    public static void main(String[] args) throws IOException, FormatException {
        //NativeLibraryLoader.loadLibraryFromJar("/native/blosc.dll");

        String path = "E:\\zarr\\4495402.zarr";
        ZarrReader r = new ZarrReader();
        r.setId(path);
        System.out.println(r.getSizeX()+" x "+r.getSizeY()+"   "+r.getOptimalTileWidth()+"   "+r.getOptimalTileHeight()+"  "+r.getSeriesCount());
        
        r.setSeries(0);
        BufferedImageReader reader = new BufferedImageReader(r);
        BufferedImage img = reader.openImage(0, 750000, 150000, 25000, 25000);
        
        File outputFile = new File("E:\\zarr\\output.png");
        ImageIO.write(img, "png", outputFile);
    }
    
}
