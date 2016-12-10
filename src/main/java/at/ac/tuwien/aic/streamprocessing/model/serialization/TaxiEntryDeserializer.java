package at.ac.tuwien.aic.streamprocessing.model.serialization;

import at.ac.tuwien.aic.streamprocessing.model.TaxiEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Base64;

/**
 * Deserializer for taxi entries.
 */
public class TaxiEntryDeserializer {
    private final static Logger logger = LoggerFactory.getLogger(TaxiEntryDeserializer.class);

    /**
     * Deserialize a taxi entry.
     *
     * @param bytes the bytes to deserialize.
     * @return the corresponding taxi entry
     */
    public static TaxiEntry deserialize(byte[] bytes) {
        try (ByteArrayInputStream byteArrayStream = new ByteArrayInputStream(Base64.getDecoder().decode(bytes))) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayStream)) {
                return (TaxiEntry) objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                System.out.println("Failed to deserialize TaxiEntry" + e);
                return null;
            }
        } catch (IOException e) {
            System.out.println("Failed to deserialize TaxiEntry" + e);
            return null;
        }
    }
}
