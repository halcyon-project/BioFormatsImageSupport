package com.ebremer.halcyon.filereaders;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class SVSImageReaderFactory implements FileReaderFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(SVSImageReaderFactory.class);

    @Override
    public FileReader create(URI uri, URI base) {
        try {
            return new SVSImageReader(uri, base);
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
        set.add("svs");
        return set;
    }
}
