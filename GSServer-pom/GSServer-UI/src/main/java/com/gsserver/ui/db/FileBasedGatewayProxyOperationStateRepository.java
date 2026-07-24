package com.gsserver.ui.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsserver.ui.gateway.GatewayProxyOperationState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileBasedGatewayProxyOperationStateRepository {
  private static final Logger logger = LoggerFactory.getLogger(FileBasedGatewayProxyOperationStateRepository.class);
  private final ObjectMapper objectMapper;
  private final Path storageDir;

  public FileBasedGatewayProxyOperationStateRepository(
      @Value("${gsserver.storage.gateway.dir:./data/gateway}") String storageDir,
      ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.storageDir = Path.of(storageDir);
    ensureStorageDir();
  }

  private void ensureStorageDir() {
    try {
      Files.createDirectories(storageDir);
    } catch (IOException e) {
      logger.error("Failed to create storage directory: {}", storageDir, e);
      throw new RuntimeException("Cannot create storage directory", e);
    }
  }

  public void save(GatewayProxyOperationState state) {
    try {
      Path filePath = storageDir.resolve(state.operationId() + ".json");
      String json = objectMapper.writeValueAsString(state);
      Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
      updateLatestLink(state);
      logger.debug("Saved gateway proxy operation state: {}", state.operationId());
    } catch (IOException e) {
      logger.error("Failed to save gateway proxy operation state", e);
      throw new RuntimeException("Cannot save operation state", e);
    }
  }

  public Optional<GatewayProxyOperationState> getLatest() {
    try {
      Path latestFile = storageDir.resolve("latest.json");
      if (!Files.exists(latestFile)) {
        return Optional.empty();
      }
      String json = Files.readString(latestFile);
      GatewayProxyOperationState state = objectMapper.readValue(json, GatewayProxyOperationState.class);
      return Optional.of(state);
    } catch (IOException e) {
      logger.error("Failed to read latest gateway proxy operation state", e);
      return Optional.empty();
    }
  }

  public Optional<GatewayProxyOperationState> getById(String operationId) {
    try {
      Path filePath = storageDir.resolve(operationId + ".json");
      if (!Files.exists(filePath)) {
        return Optional.empty();
      }
      String json = Files.readString(filePath);
      GatewayProxyOperationState state = objectMapper.readValue(json, GatewayProxyOperationState.class);
      return Optional.of(state);
    } catch (IOException e) {
      logger.error("Failed to read gateway proxy operation state: {}", operationId, e);
      return Optional.empty();
    }
  }

  public void delete(String operationId) {
    try {
      Path filePath = storageDir.resolve(operationId + ".json");
      Files.deleteIfExists(filePath);
      logger.debug("Deleted gateway proxy operation state: {}", operationId);
    } catch (IOException e) {
      logger.error("Failed to delete gateway proxy operation state: {}", operationId, e);
    }
  }

  private void updateLatestLink(GatewayProxyOperationState state) {
    try {
      Path latestFile = storageDir.resolve("latest.json");
      String json = objectMapper.writeValueAsString(state);
      Files.writeString(latestFile, json, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      logger.error("Failed to update latest link", e);
    }
  }
}
