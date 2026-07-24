package com.gsserver.ui.gateway.adapter;

public interface NginxCommandExecutor {
  NginxExecutionResult configure(NginxConfigurationCommand command);

  NginxExecutionResult rollback(String previousConfigBackupPath);
}
