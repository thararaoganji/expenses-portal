package portal.expenses.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);

    @Value("${storage.local.upload-dir}")
    private String uploadDir;

    /**
     * Uploads a file to local storage.
     */
    public void uploadFile(String objectKey, byte[] fileBytes) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        
        // Create directories if they don't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }

        // Create file path
        Path filePath = uploadPath.resolve(objectKey);
        
        // Create parent directories for the file if needed
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // Write file
        Files.write(filePath, fileBytes);
        logger.info("File uploaded to local storage: {}", filePath.toAbsolutePath());
    }

    /**
     * Downloads a file from local storage.
     */
    public byte[] downloadFile(String objectKey) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(objectKey);
            if (!Files.exists(filePath)) {
                throw new java.io.UncheckedIOException(new java.io.FileNotFoundException("File not found: " + objectKey));
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed to download file: " + objectKey, e);
        }
    }

    /**
     * Deletes a file from local storage.
     */
    public void deleteFile(String objectKey) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(objectKey);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("File deleted from local storage: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException("Failed to delete file: " + objectKey, e);
        }
    }
}
