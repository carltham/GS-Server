package com.gsserver.ui.db;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsserver.ui.hardening.HardeningOperationState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileBasedHardeningOperationStateRepositoryTest {
  @TempDir Path tempDir;
  private FileBasedHardeningOperationStateRepository repository;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    repository = new FileBasedHardeningOperationStateRepository(tempDir.toString(), objectMapper);
  }

  @Test
  void save_createsOperationFile() {
    HardeningOperationState state = new HardeningOperationState(
        "op-123", "2026-07-24T10:00:00Z", "SUCCESS", "tenant-1", "user-1", "baseline", "Linux", 0, false, "none", "Hardening completed");

    repository.save(state);

    Path operationFile = tempDir.resolve("op-123.json");
    assertTrue(Files.exists(operationFile));
  }

  @Test
  void save_createsLatestLink() {
    HardeningOperationState state = new HardeningOperationState(
        "op-123", "2026-07-24T10:00:00Z", "SUCCESS", "tenant-1", "user-1", "baseline", "Linux", 0, false, "none", "Hardening completed");

    repository.save(state);

    Path latestFile = tempDir.resolve("latest.json");
    assertTrue(Files.exists(latestFile));
  }

  @Test
  void getLatest_returnsLatestOperation() {
    HardeningOperationState state = new HardeningOperationState(
        "op-123", "2026-07-24T10:00:00Z", "SUCCESS", "tenant-1", "user-1", "baseline", "Linux", 0, false, "none", "Hardening completed");

    repository.save(state);
    var result = repository.getLatest();

    assertTrue(result.isPresent());
    assertEquals("op-123", result.get().operationId());
    assertEquals("SUCCESS", result.get().status());
  }

  @Test
  void getLatest_returnsEmptyWhenNoState() {
    var result = repository.getLatest();
    assertTrue(result.isEmpty());
  }

  @Test
  void save_overwritesPreviousLatest() {
    HardeningOperationState state1 = new HardeningOperationState(
        "op-123", "2026-07-24T10:00:00Z", "SUCCESS", "tenant-1", "user-1", "baseline", "Linux", 0, false, "none", "First");
    HardeningOperationState state2 = new HardeningOperationState(
        "op-124", "2026-07-24T11:00:00Z", "FAILED", "tenant-1", "user-1", "strict", "Linux", 1, false, "none", "Second");

    repository.save(state1);
    repository.save(state2);

    var result = repository.getLatest();
    assertTrue(result.isPresent());
    assertEquals("op-124", result.get().operationId());
  }

  @Test
  void delete_removesOperationFile() {
    HardeningOperationState state = new HardeningOperationState(
        "op-123", "2026-07-24T10:00:00Z", "SUCCESS", "tenant-1", "user-1", "baseline", "Linux", 0, false, "none", "Hardening completed");

    repository.save(state);
    Path operationFile = tempDir.resolve("op-123.json");
    assertTrue(Files.exists(operationFile));

    repository.delete("op-123");
    assertFalse(Files.exists(operationFile));
  }

  @Test
  void getLatest_deserializesJsonCorrectly() throws IOException {
    HardeningOperationState original = new HardeningOperationState(
        "op-123", "2026-07-24T10:00:00Z", "SUCCESS", "tenant-1", "user-1", "baseline", "Linux", 0, false, "none", "Hardening completed");

    repository.save(original);
    var result = repository.getLatest();

    assertTrue(result.isPresent());
    HardeningOperationState loaded = result.get();
    assertEquals("op-123", loaded.operationId());
    assertEquals("2026-07-24T10:00:00Z", loaded.occurredAtUtc());
    assertEquals("SUCCESS", loaded.status());
    assertEquals("tenant-1", loaded.tenantId());
    assertEquals("user-1", loaded.requestedBy());
    assertEquals("baseline", loaded.profile());
    assertEquals("Linux", loaded.platform());
    assertEquals(0, loaded.exitCode());
    assertFalse(loaded.timedOut());
    assertEquals("none", loaded.rollbackStatus());
    assertEquals("Hardening completed", loaded.message());
  }
}
