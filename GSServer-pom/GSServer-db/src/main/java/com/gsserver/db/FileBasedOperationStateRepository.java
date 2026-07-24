package com.gsserver.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FileBasedOperationStateRepository<T> implements OperationStateRepository<T> {
  private static final Logger logger = LoggerFactory.getLogger(FileBasedOperationStateRepository.class);
  protected final ObjectMapper objectMapper;
  protected final Path storageDir;
  protected final Class<T> stateClass;

  public FileBasedOperationStateRepository(Path storageDir, Class<T> stateClass, ObjectMapper objectMapper) {
    this.storageDir = storageDir;
    this.stateClass = stateClass;
    this.objectMapper = objectMapper;
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

  protected abstract String getStateFileName(T state);

  @Override
  public void save(T state) {
    try {
      String fileName = getStateFileName(state);
      Path filePath = storageDir.resolve(fileName);
      String json = objectMapper.writeValueAsString(state);
      Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
      logger.debug("Saved operation state: {}", fileName);
    } catch (IOException e) {
      logger.error("Failed to save operation state", e);
      throw new RuntimeException("Cannot save operation state", e);
    }
  }

  @Override
  public Optional<T> getLatest() {
    try {
      Path latestFile = storageDir.resolve("latest.json");
      if (!Files.exists(latestFile)) {
        return Optional.empty();
      }
      String json = Files.readString(latestFile);
      T state = objectMapper.readValue(json, stateClass);
      return Optional.of(state);
    } catch (IOException e) {
      logger.error("Failed to read latest operation state", e);
      return Optional.empty();
    }
  }

  @Override
  public void delete(String operationId) {
    try {
      Path filePath = storageDir.resolve(operationId + ".json");
      Files.deleteIfExists(filePath);
      logger.debug("Deleted operation state: {}", operationId);
    } catch (IOException e) {
      logger.error("Failed to delete operation state: {}", operationId, e);
    }
  }

  protected void updateLatestLink(T state) {
    try {
      Path latestFile = storageDir.resolve("latest.json");
      String json = objectMapper.writeValueAsString(state);
      Files.writeString(latestFile, json, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      logger.error("Failed to update latest link", e);
    }
  }
}
