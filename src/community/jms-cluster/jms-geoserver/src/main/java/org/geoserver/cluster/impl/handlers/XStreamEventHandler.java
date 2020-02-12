/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import com.thoughtworks.xstream.XStream;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geotools.util.logging.Logging;

/**
 * Base {@link JMSEventHandler} that uses {@link XStream} for serialization and deserialization of
 * event objects
 */
public abstract class XStreamEventHandler<O> extends JMSEventHandler<String, O> {

    protected static final java.util.logging.Logger LOGGER =
            Logging.getLogger(XStreamEventHandler.class);

    protected final XStream xstream;

    /**
     * @param xstream an already initialized xstream
     * @param generatorClass the SPI class which generate this kind of handler
     */
    public XStreamEventHandler(
            final XStream xstream, Class<? extends JMSEventHandlerSPI<String, O>> generatorClass) {
        super(generatorClass);
        this.xstream = xstream;
        configureXStream(xstream);
    }

    /**
     * here you may modify XStream [de]serialization adding omitFields and all other changes
     * supported by XStream
     *
     * @param xstream a not null and initted XStream to use
     */
    protected void configureXStream(final XStream xstream) {
        // subclasses shall override as needed
    }

    /** Uses the provided {@link XStream} instance to serialize the event object */
    public String serialize(O o) throws Exception {
        return xstream.toXML(o);
    }

    /** Uses the provided {@link XStream} instance to deserialize the event object */
    @SuppressWarnings("unchecked")
    public O deserialize(String o) throws Exception {
        final Object source = xstream.fromXML(o);
        if (source != null) {
            return (O) source;
        } else {
            throw new IllegalArgumentException(
                    this.getClass().getCanonicalName()
                            + " is unable to deserialize the object:"
                            + o);
        }
    }
}
