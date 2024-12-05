package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.lib.URITools;
import com.ebremer.halcyon.lib.ImageMeta;
import com.ebremer.halcyon.lib.ImageRegion;
import com.ebremer.halcyon.lib.Rectangle;
import com.ebremer.ns.EXIF;
import com.ebremer.ns.HAL;
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
import loci.formats.in.NDPIReader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.XSD;

/**
 *
 * @author erich
 */
public class NDPIImageReader extends AbstractImageReader {
    private BufferedImageReader reader;
    private final ImageMeta meta;
    private final URI uri;
    private static final int METAVERSION = 0;
    
    public NDPIImageReader(URI uri, URI base) throws IOException {
        this.uri = uri;
        reader = new BufferedImageReader(new NDPIReader());
        File file = new File(uri);
        try {
            reader.setId(file.toString());
        } catch (FormatException ex) {
            System.out.println("Format Issue Reading File : "+file);
        }
        reader.setSeries(0);
        ImageMeta.Builder builder = ImageMeta.Builder.getBuilder(0, reader.getSizeX(), reader.getSizeY())
                .setTileSizeX(reader.getOptimalTileWidth())
                .setTileSizeY(reader.getOptimalTileHeight());
        for(int i=0; i<reader.getSeriesCount();i++) {
            reader.setSeries(i);
            builder.addScale(i, reader.getSizeX(), reader.getSizeY());
        }
        meta = builder.build();
    }

    @Override
    public String getFormat() {
        return "ndpi";
    }
    
    @Override
    public int getMetaVersion() {
        return METAVERSION;
    }

    private BufferedImage readTile(ImageRegion region, int series) {
        reader.setSeries(series);
        try {
            return reader.openImage(0, region.getX(), region.getY(), region.getWidth(), region.getHeight());
        } catch (FormatException ex) {
            Logger.getLogger(NDPIImageReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NDPIImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(NDPIImageReader.class.getName()).log(Level.SEVERE, null, ex);
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
    public Model getMeta(URI xuri) {
        Model m = ModelFactory.createDefaultModel();
        m.createResource(URITools.fix(xuri))
            .addLiteral(HAL.filemetaversion, m.createTypedLiteral( METAVERSION, XSD.integer.getURI()))
            .addLiteral(EXIF.width, m.createTypedLiteral(meta.getWidth(), XSD.integer.getURI()))
            .addLiteral(EXIF.height, m.createTypedLiteral(meta.getHeight(), XSD.integer.getURI()))
            .addProperty(RDF.type, SchemaDO.ImageObject);
        return m;
    }
    
    @Override
    public Model getMeta() {                
        return getMeta(uri);
    }
    
    @Override
    public Set<String> getSupportedFormats() {
        Set<String> set = new HashSet<>();
        set.add("ndpi");
        return set;
    }

    @Override
    public Model readTileMeta(ImageRegion region, Rectangle preferredsize) {
        return ModelFactory.createDefaultModel();
    }
}
