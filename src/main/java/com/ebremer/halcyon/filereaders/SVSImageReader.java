package com.ebremer.halcyon.filereaders;

import com.ebremer.halcyon.lib.URITools;
import com.ebremer.halcyon.lib.ImageMeta;
import com.ebremer.halcyon.lib.ImageRegion;
import com.ebremer.halcyon.lib.Rectangle;
import com.ebremer.ns.EXIF;
import com.ebremer.ns.HAL;
import com.ebremer.ns.LWS;
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
import loci.formats.in.SVSReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.ome.OMEPyramidStore;
import loci.formats.services.OMEXMLService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SchemaDO;
import org.apache.jena.vocabulary.XSD;

/**
 *
 * @author erich
 */
public class SVSImageReader extends AbstractImageReader {
    private BufferedImageReader reader;
    private final ImageMeta meta;
    private final URI uri;
    private static final int METAVERSION = 0;
    private final long sizeInBytes;
    
    public SVSImageReader(URI uri, URI base) throws IOException {
        this.uri = uri;
        loci.common.DebugTools.setRootLevel("WARN");        
        reader = new BufferedImageReader(new SVSReader());
        File file = new File(uri);
        sizeInBytes = file.length();
        try {
            reader.setId(file.toString());
        } catch (FormatException ex) {
            System.out.println("Format Issue Reading File : "+file+"\n"+ex.toString());
        }
        reader.setSeries(0);
        ImageMeta.Builder builder = ImageMeta.Builder.getBuilder(0, reader.getSizeX(), reader.getSizeY())
                .setTileSizeX(reader.getOptimalTileWidth())
                .setTileSizeY(reader.getOptimalTileHeight());
        for(int i=0; i<reader.getSeriesCount();i++) {
            reader.setSeries(i);
            builder.addScale(i, reader.getSizeX(), reader.getSizeY());
        }
        //System.out.println("# of scales "+reader.getSeriesCount());
        meta = builder.build();
        //ShowPyramidMeta();
    }
    
    private void ShowPyramidMeta() {
        meta.getScales().forEach(s->{
            System.out.println("Scale ==> "+s.width()+" "+s.height());
        });
    }
    
    public void calculateMeta() throws DependencyException, ServiceException {
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata omexml = service.createOMEXMLMetadata();
        MetadataStore mx = reader.getReader().getMetadataStore();
        OMEPyramidStore ha = (OMEPyramidStore) mx;
        /*
        String objectiveID = mx.getObjectiveSettingsID(0);
        Double magnification =  0d;
	int objectiveIndex = -1;
	int instrumentIndex = -1;
	int nInstruments = mx.getInstrumentCount();
	for (int i = 0; i < nInstruments; i++) {
            int nObjectives = mx.getObjectiveCount(i);
            for (int o = 0; 0 < nObjectives; o++) {
                if (objectiveID.equals(mx.getObjectiveID(i, o))) {
                    instrumentIndex = i;
                    objectiveIndex = o;
                    break;
		}
            }	    		
	}
	if (instrumentIndex < 0) {
            //logger.warn("Cannot find objective for ref {}", objectiveID);
	} else {
            Double magnificationObject = mx.getObjectiveNominalMagnification(instrumentIndex, objectiveIndex);
            if (magnificationObject == null) {
                //logger.warn("Nominal objective magnification missing for {}:{}", instrumentIndex, objectiveIndex);
            } else {
                magnification = magnificationObject;		    		
            }
        }
        System.out.println(magnification);*/
    }

    @Override
    public String getFormat() {
        return "svs";
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
    public Model getMeta(URI xuri) {
        Model m = ModelFactory.createDefaultModel();
        Resource bnode = m.createResource();
        m.createResource(URITools.fix(xuri))
            .addProperty(RDF.type, LWS.DataResource)
            .addProperty(LWS.representation, bnode)
            .addLiteral(HAL.filemetaversion, m.createTypedLiteral( METAVERSION, XSD.integer.getURI()))
            .addLiteral(EXIF.width, m.createTypedLiteral(meta.getWidth(), XSD.integer.getURI()))
            .addLiteral(EXIF.height, m.createTypedLiteral(meta.getHeight(), XSD.integer.getURI()))
            .addProperty(RDF.type, SchemaDO.ImageObject);
        bnode
            .addProperty(LWS.mediaType, "application/octet-stream")
            .addLiteral(LWS.sizeInBytes, sizeInBytes);
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
        File file = new File("D:\\HalcyonStorage\\hamid\\SP16-22530_1I_1.svs");
        SVSImageReader reader = new SVSImageReader(file.toURI(), file.toURI());       
        //RDFDataMgr.write(System.out, reader.getMeta(), Lang.TURTLE);
        //reader.calculateMeta();
    }
}
