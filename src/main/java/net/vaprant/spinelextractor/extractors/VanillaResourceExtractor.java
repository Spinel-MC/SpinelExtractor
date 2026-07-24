package net.vaprant.spinelextractor.extractors;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public final class VanillaResourceExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(VanillaResourceExtractor.class);
    private static final String OUTPUT_DIRECTORY_PROPERTY = "spinelExtractor.exampleServerAssets";
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Paths.get("../ExampleServer/assets");
    private static final String CLIENT_TRANSLATION_RESOURCE_PATH = "assets/minecraft/lang/en_us.json";

    public static void extract(MinecraftServer server) {
        VanillaResourceExtractor extractor = new VanillaResourceExtractor(server);
        extractor.exportVanillaDatapack();
        extractor.exportClientTranslation();
    }

    private final MinecraftServer server;

    private VanillaResourceExtractor(MinecraftServer server) {
        this.server = server;
    }

    private void exportVanillaDatapack() {
        ResourceManager resourceManager = server.getResourceManager();
        Map<Identifier, Resource> datapackResources =
                resourceManager.listResources("", identifier -> identifier.getNamespace().equals("minecraft"));
        datapackResources.forEach(this::exportDatapackResource);
    }

    private void exportDatapackResource(Identifier identifier, Resource resource) {
        Path destination = outputDirectory()
                .resolve("data")
                .resolve(identifier.getNamespace())
                .resolve(identifier.getPath());
        copyResource(resource, destination);
    }

    private void exportClientTranslation() {
        InputStream translationResource = VanillaResourceExtractor.class
                .getClassLoader()
                .getResourceAsStream(CLIENT_TRANSLATION_RESOURCE_PATH);
        if (translationResource == null) {
            LOG.error("Missing Vanilla client resource {}", CLIENT_TRANSLATION_RESOURCE_PATH);
            return;
        }
        Path destination = outputDirectory().resolve(CLIENT_TRANSLATION_RESOURCE_PATH);
        try (InputStream resourceStream = translationResource) {
            copyStream(resourceStream, destination);
        } catch (IOException storageException) {
            LOG.error("Failed to export Vanilla client resource {}", CLIENT_TRANSLATION_RESOURCE_PATH, storageException);
        }
    }

    private void copyResource(Resource resource, Path destination) {
        try (InputStream resourceStream = resource.open()) {
            copyStream(resourceStream, destination);
        } catch (IOException storageException) {
            LOG.error("Failed to export Vanilla resource {}", destination, storageException);
        }
    }

    private void copyStream(InputStream resourceStream, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.copy(resourceStream, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Exported Vanilla resource {}", destination.toAbsolutePath());
    }

    private Path outputDirectory() {
        return Paths.get(System.getProperty(OUTPUT_DIRECTORY_PROPERTY, DEFAULT_OUTPUT_DIRECTORY.toString()));
    }
}
