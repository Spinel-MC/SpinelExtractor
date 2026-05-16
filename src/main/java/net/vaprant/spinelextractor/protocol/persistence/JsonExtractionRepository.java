package net.vaprant.spinelextractor.protocol.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class JsonExtractionRepository {

    private static final Logger LOG = LoggerFactory.getLogger(JsonExtractionRepository.class);
    private final String outputFilePath;

    public JsonExtractionRepository(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public void save(Map<String, Object> extractionData) {
        Gson jsonSerializer = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        try {
            Path fileOutputPath = Paths.get(outputFilePath);
            Path parentDirectory = fileOutputPath.getParent();

            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            String serializedJson = jsonSerializer.toJson(extractionData);
            Files.writeString(fileOutputPath, serializedJson);
            
            Path absoluteFileLocation = fileOutputPath.toAbsolutePath();
            LOG.info("Successfully saved extraction data to {}", absoluteFileLocation);
        } catch (IOException storageException) {
            LOG.error("Failed to persist extraction JSON", storageException);
        }
    }
}
