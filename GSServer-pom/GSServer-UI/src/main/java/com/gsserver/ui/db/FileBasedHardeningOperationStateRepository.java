package com.gsserver.ui.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gsserver.db.FileBasedOperationStateRepository;
import com.gsserver.ui.hardening.HardeningOperationState;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileBasedHardeningOperationStateRepository extends FileBasedOperationStateRepository<HardeningOperationState> {
  public FileBasedHardeningOperationStateRepository(
      @Value("${gsserver.storage.hardening.dir:./data/hardening}") String storageDir,
      ObjectMapper objectMapper) {
    super(Path.of(storageDir), HardeningOperationState.class, objectMapper);
  }

  @Override
  protected String getStateFileName(HardeningOperationState state) {
    return state.operationId() + ".json";
  }

  @Override
  public void save(HardeningOperationState state) {
    super.save(state);
    updateLatestLink(state);
  }
}
