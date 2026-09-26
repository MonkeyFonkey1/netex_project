package com.netex.addressbook.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PictureStorage {

    private static final Logger logger = LoggerFactory.getLogger(PictureStorage.class);
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000;

    private final Path directory;

    public PictureStorage(@Value("${app.picture-directory}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a picture");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Picture exceeds 5 MB");
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Picture exceeds 5 MB");
            }
            String extension = detectImageType(bytes);
            String filename = UUID.randomUUID() + extension;
            Files.createDirectories(directory);
            Files.write(directory.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
            return filename;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not store picture", exception);
        }
    }

    public byte[] read(String filename) {
        try {
            return Files.readAllBytes(safePath(filename));
        } catch (NoSuchFileException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Picture not found", exception);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read picture", exception);
        }
    }

    public MediaType contentType(String filename) {
        safePath(filename);
        return filename.toLowerCase(Locale.ROOT).endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    public void deleteIfExists(String filename) {
        if (filename == null) {
            return;
        }
        try {
            Files.deleteIfExists(safePath(filename));
        } catch (IOException exception) {
            // The database already points elsewhere. An orphaned file can be cleaned up later.
            logger.warn("Could not delete old picture {}", filename, exception);
        }
    }

    private Path safePath(String filename) {
        if (filename == null || !filename.matches("[A-Za-z0-9._-]+\\.(jpg|png)")) {
            throw new IllegalArgumentException("Invalid stored picture name");
        }
        Path file = directory.resolve(filename).normalize();
        if (!directory.equals(file.getParent())) {
            throw new IllegalArgumentException("Invalid stored picture path");
        }
        return file;
    }

    private String detectImageType(byte[] bytes) {
        try (MemoryCacheImageInputStream input =
                new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a JPEG or PNG picture");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a JPEG or PNG picture");
                }
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels == 0 || pixels > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Picture dimensions are too large");
                }
                // The header alone can look valid even when the image data is truncated.
                if (reader.read(0) == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid picture file");
                }
                return format.equalsIgnoreCase("PNG") ? ".png" : ".jpg";
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid picture file", exception);
        }
    }
}
