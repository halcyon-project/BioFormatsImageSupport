package com.ebremer.halcyon.filereaders;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class ZarrImageReaderFactory implements FileReaderFactory {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ZarrImageReaderFactory.class);

    @Override
    public FileReader create(URI uri, URI base) {
        try {
            return new ZarrImageReader(uri, base);
        } catch (IOException ex) {
            logger.error(uri+" "+base+" "+ex.toString());
        }
        return null;
    }

    @Override
    public FileReader create(SeekableByteChannel src, URI base) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> getSupportedFormats() {
        Set<String> set = new HashSet<>();
        set.add("zarr");
        return set;
    }
}
