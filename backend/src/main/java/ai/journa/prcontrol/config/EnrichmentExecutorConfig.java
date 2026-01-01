package ai.journa.prcontrol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class EnrichmentExecutorConfig {
  @Bean(name = "enrichmentTaskExecutor")
  @Primary
  public TaskExecutor enrichmentTaskExecutor(EnrichmentProperties properties) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    int workers = Math.max(1, properties.getRunner().getMaxConcurrentTasks());
    int queueCapacity = Math.max(1, properties.getRunner().getBatchSize());
    executor.setCorePoolSize(workers);
    executor.setMaxPoolSize(workers);
    executor.setQueueCapacity(queueCapacity);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setThreadNamePrefix("enrich-");
    executor.initialize();
    return executor;
  }
}
