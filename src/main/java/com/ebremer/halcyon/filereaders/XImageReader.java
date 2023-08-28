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
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.gui.BufferedImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.ome.OMEPyramidStore;
import loci.formats.services.OMEXMLService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;

/**
 *
 * @author erich
 */
public class XImageReader extends AbstractImageReader {
    private BufferedImageReader reader;
    private final ImageMeta meta;
    private final URI uri;
    private static final int METAVERSION = 0;
    
    public XImageReader(URI uri) throws IOException {
        this.uri = uri;
        loci.common.DebugTools.setRootLevel("WARN");
        reader = new BufferedImageReader(new loci.formats.ImageReader());
        OMEXMLService service = null;
        try {
            ServiceFactory factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            IMetadata xmeta = service.createOMEXMLMetadata();
            reader.setMetadataStore(xmeta);
        } catch (DependencyException ex) {
            Logger.getLogger(XImageReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            Logger.getLogger(XImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }                
        File file = new File(uri);
        try {
            reader.setId(file.toString());
        } catch (FormatException ex) {
            System.out.println("Format Issue Reading File : "+file+"\n"+ex.toString());
        }
        reader.setSeries(0);
        try {
            String xml = service.getOMEXML(service.asRetrieve(reader.getMetadataStore()));
            System.out.println(xml);
        } catch (ServiceException ex) {
            Logger.getLogger(XImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        ImageMeta.Builder builder = ImageMeta.Builder.getBuilder(0, reader.getSizeX(), reader.getSizeY())
                .setTileSizeX(reader.getOptimalTileWidth())
                .setTileSizeY(reader.getOptimalTileHeight());
        for(int i=0; i<reader.getSeriesCount();i++) {
            reader.setSeries(i);
            builder.addScale(i, reader.getSizeX(), reader.getSizeY());
        }
        meta = builder.build();
    }
    
    public Double FindMagnification() {
        var mx = (OMEPyramidStore) reader.getReader().getMetadataStore();
        System.out.println(mx.dumpXML());
        String objectiveID = mx.getObjectiveSettingsID(0);
	int objectiveIndex = -1;
	int instrumentIndex = -1;
	int numberOfInstruments = mx.getInstrumentCount();
	for (int ii = 0; ii < numberOfInstruments; ii++) {
            int numObjectives = mx.getObjectiveCount(ii);
            for (int oi = 0; 0 < numObjectives; oi++) {
                if (objectiveID.equals(mx.getObjectiveID(ii, oi))) {
                    instrumentIndex = ii;
                    objectiveIndex = oi;
                    break;
		}
            }	    		
	}
	return (instrumentIndex < 0) ? null : mx.getObjectiveNominalMagnification(instrumentIndex, objectiveIndex);
    }

    @Override
    public String getFormat() {
        return "Xsvs";
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
            Logger.getLogger(XImageReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XImageReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(XImageReader.class.getName()).log(Level.SEVERE, null, ex);
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
            .addLiteral(HAL.filemetaversion, METAVERSION)
            .addLiteral(EXIF.width, meta.getWidth())
            .addLiteral(EXIF.height, meta.getHeight())
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
        set.add("svs");
        return set;
    }

    @Override
    public Model readTileMeta(ImageRegion region, Rectangle preferredsize) {
        return ModelFactory.createDefaultModel();
    }
    
    public static void main(String[] args) throws IOException, DependencyException, ServiceException {
        File file = new File("D:\\HalcyonStorage\\tcga\\brca\\TCGA-E2-A1B1-01Z-00-DX1.7C8DF153-B09B-44C7-87B8-14591E319354.svs");
        //File file = new File("D:\\HalcyonStorage\\tcga\\brca\\tif\\TCGA-E2-A1B1-01Z-00-DX1.7C8DF153-B09B-44C7-87B8-14591E319354.tif");
        XImageReader reader = new XImageReader(file.toURI());       
        RDFDataMgr.write(System.out, reader.getMeta(), Lang.TURTLE);
        reader.FindMagnification();
    }
}
