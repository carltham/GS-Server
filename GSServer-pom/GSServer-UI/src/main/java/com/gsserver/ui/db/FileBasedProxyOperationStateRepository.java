package com.gsserver.ui.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsserver.db.FileBasedOperationStateRepository;
import com.gsserver.ui.proxy.ProxyOperationState;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileBasedProxyOperationStateRepository extends FileBasedOperationStateRepository<ProxyOperationState> {
  public FileBasedProxyOperationStateRepository(
      @Value("${gsserver.storage.proxy.dir:./data/proxy}") String storageDir,
      ObjectMapper objectMapper) {
    super(Path.of(storageDir), ProxyOperationState.class, objectMapper);
  }

  @Override
  protected String getStateFileName(ProxyOperationState state) {
    return state.operationId() + ".json";
  }

  @Override
  public void save(ProxyOperationState state) {
    super.save(state);
    updateLatestLink(state);
  }
}
