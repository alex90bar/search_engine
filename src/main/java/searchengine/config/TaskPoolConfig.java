package searchengine.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * TaskPoolConfig
 *
 * @author alex90bar
 */

@EnableAsync
@Configuration
public class TaskPoolConfig {

    @Value(value = "${java.concurrent.corePoolSize}")
    private Integer corePoolSize;
    @Value(value = "${java.concurrent.maximumPoolSize}")
    private Integer maxPoolSize;
    @Value(value = "${java.concurrent.queueCapacity}")
    private Integer queueCapacity;
    @Value(value = "${java.concurrent.keepAliveSeconds}")
    private Integer keepAliveSeconds;

    @Bean("taskExecutor") // имя пула потоков
    public Executor taskExecutor() {
        // Используем инкапсулированный в Spring пул асинхронных потоков
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // Инициализируем количество потоков
        executor.setMaxPoolSize(maxPoolSize); // Максимальное количество потоков
        executor.setQueueCapacity(queueCapacity); // буферная очередь
        executor.setKeepAliveSeconds(keepAliveSeconds); // Разрешить время простоя в секунду
        executor.setThreadNamePrefix("taskExecutor -");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(keepAliveSeconds);
        executor.initialize(); // Инициализируем
        return executor;
    }
}

