package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.datum.URITools;
import com.ebremer.halcyon.lib.ImageMeta;
import com.ebremer.halcyon.lib.ImageRegion;
import com.ebremer.halcyon.lib.Rectangle;
import com.ebremer.ns.EXIF;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FormatException;
import loci.formats.gui.BufferedImageReader;
import loci.formats.in.SVSReader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

/**
 *
 * @author erich
 */
public class SVSImageReader extends AbstractImageReader {
    private BufferedImageReader reader;
    private final ImageMeta meta;
    private final URI uri;
    
    public SVSImageReader(URI uri) throws IOException {
        loci.common.DebugTools.setRootLevel("WARN");
        this.uri = uri;
        reader = new BufferedImageReader(new SVSReader());
        File file = new File(uri);
        try {
            reader.setId(file.toString());
        } catch (FormatException ex) {
            Logger.getLogger(SVSImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        reader.setSeries(0);
        ImageMeta.Builder builder = ImageMeta.Builder.getBuilder(0, reader.getSizeX(), reader.getSizeY())
                .setTileSizeX(reader.getOptimalTileWidth())
                .setTileSizeY(reader.getOptimalTileHeight());
        for(int i=1; i<reader.getSeriesCount();i++) {
            reader.setSeries(i);
            builder.addScale(i, reader.getSizeX(), reader.getSizeY());
        }
        meta = builder.build();
    }

    @Override
    public String getFormat() {
        return "svs";
    }

    private BufferedImage readTile(ImageRegion region, int series) {
        reader.setSeries(series);
        try {
            return reader.openImage(0, region.getX(), region.getY(), region.getWidth(), region.getHeight());
        } catch (FormatException ex) {
            Logger.getLogger(SVSImageReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SVSImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(SVSImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public BufferedImage readTile(ImageRegion region, Rectangle preferredsize) {
        ImageMeta.ImageScale scale = meta.getBestMatch(Math.max((double) region.getWidth()/(double) preferredsize.width(),(double) region.getHeight()/ (double) preferredsize.height()));
        return readTile(scale.Validate(region.scaleRegion(scale.scale())),scale.series());
    }
    
    @Override
    public ImageMeta getImageMeta() {
        return meta;
    }

    @Override
    public Model getMeta() {
        Model m = ModelFactory.createDefaultModel();
        m.createResource(URITools.fix(uri))
            .addLiteral(EXIF.width, meta.getWidth())
            .addLiteral(EXIF.height, meta.getHeight())
            .addProperty(RDF.type, SchemaDO.ImageObject);
        return m;
    }
    
    @Override
    public Set<String> getSupportedFormats() {
        Set<String> set = new HashSet<>();
        set.add("svs");
        return set;
    }
}

/*
    public static void main(String[] args) {
        loci.common.DebugTools.setRootLevel("WARN");
    }
*/

        /*
        DynamicMetadataOptions options = new DynamicMetadataOptions();
        options.setValidate(false);
        options.setMetadataLevel(MetadataLevel.NO_OVERLAYS);
        try (ImageReader reader = new ImageReader()) {
            reader.setMetadataOptions(options);
            reader.setOriginalMetadataPopulated(false);
            reader.setId(file.getPath());
            Resource ss = m.createResource(EB.fix(file));
            m.add(ss,RDF.type,SchemaDO.ImageObject);
            m.addLiteral(ss,EXIF.width,reader.getSizeX());
            m.addLiteral(ss,EXIF.height,reader.getSizeY());
            reader.close();
        } catch (FormatException | IOException ex) {
            
        }
        return m;
*/