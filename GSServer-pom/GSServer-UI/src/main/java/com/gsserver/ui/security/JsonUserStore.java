package com.gsserver.ui.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * File-backed store of {@link ManagedUser}s, persisted as JSON. On first start (no file yet) it is
 * seeded from the {@code gsserver.security.users} configuration so existing users carry over; after
 * that, the JSON file is the source of truth.
 *
 * <p>Registered as a bean by {@link SecurityConfig} so the auth stack is self-contained (and works
 * inside {@code @WebMvcTest} slices that import {@code SecurityConfig}).
 */
public class JsonUserStore {
  private static final Logger logger = LoggerFactory.getLogger(JsonUserStore.class);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Map<String, ManagedUser> users = new LinkedHashMap<>();

  private final Path file;
  private final SecurityUsersProperties properties;
  private final PasswordEncoder passwordEncoder;

  public JsonUserStore(SecurityUsersProperties properties, PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.passwordEncoder = passwordEncoder;
    this.file = Path.of(properties.getUsersFile());
  }

  @PostConstruct
  void init() {
    lock.writeLock().lock();
    try {
      if (Files.isRegularFile(file)) {
        load();
      } else {
        seedFromProperties();
        persist();
      }
      logger.info("User store ready ({} users) at {}", users.size(), file.toAbsolutePath());
    } finally {
      lock.writeLock().unlock();
    }
  }

  public List<ManagedUser> findAll() {
    lock.readLock().lock();
    try {
      return new ArrayList<>(users.values());
    } finally {
      lock.readLock().unlock();
    }
  }

  public Optional<ManagedUser> findByUsername(String username) {
    lock.readLock().lock();
    try {
      return Optional.ofNullable(users.get(username));
    } finally {
      lock.readLock().unlock();
    }
  }

  public boolean exists(String username) {
    lock.readLock().lock();
    try {
      return users.containsKey(username);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Insert or replace a user and persist. */
  public void save(ManagedUser user) {
    lock.writeLock().lock();
    try {
      users.put(user.username(), user);
      persist();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void delete(String username) {
    lock.writeLock().lock();
    try {
      if (users.remove(username) != null) {
        persist();
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** Count enabled users holding the given authority — used to protect the last superadmin. */
  public long countEnabledWithAuthority(String authority) {
    lock.readLock().lock();
    try {
      return users.values().stream()
          .filter(ManagedUser::enabled)
          .filter(u -> u.authorities().contains(authority))
          .count();
    } finally {
      lock.readLock().unlock();
    }
  }

  private void load() {
    try {
      ManagedUser[] loaded = objectMapper.readValue(Files.readAllBytes(file), ManagedUser[].class);
      users.clear();
      for (ManagedUser user : loaded) {
        users.put(user.username(), user);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read user store: " + file, e);
    }
  }

  private void seedFromProperties() {
    for (SecurityUsersProperties.UserEntry entry : properties.getUsers()) {
      String rawPassword = entry.getPassword() == null ? "" : entry.getPassword();
      users.put(
          entry.getUsername(),
          new ManagedUser(
              entry.getUsername(),
              passwordEncoder.encode(rawPassword),
              List.copyOf(entry.getAuthorities()),
              true));
    }
    logger.info("Seeded user store from configuration with {} users", users.size());
  }

  private void persist() {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), users.values());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write user store: " + file, e);
    }
  }
}
