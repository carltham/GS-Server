package com.gsserver.db;

import java.util.Optional;

public interface OperationStateRepository<T> {
  void save(T state);

  Optional<T> getLatest();

  void delete(String operationId);
}
