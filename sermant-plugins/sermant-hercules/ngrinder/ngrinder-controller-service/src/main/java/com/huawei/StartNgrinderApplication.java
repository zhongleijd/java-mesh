package com.huawei;

import net.grinder.engine.agent.LocalScriptTestDriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ErrorHandler;

import javax.servlet.MultipartConfigElement;
import java.io.IOException;

@SpringBootApplication(scanBasePackages = {"org.ngrinder","com.huawei"})
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJpaRepositories(basePackages = { "org.ngrinder","com.huawei" })
@EntityScan(basePackages = { "org.ngrinder","com.huawei" })
public class StartNgrinderApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartNgrinderApplication.class);
    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(StartNgrinderApplication.class, args);
    }

    @Bean
    public TaskScheduler poolScheduler() {
        ThreadPoolTaskScheduler scheduler =new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("poolScheduler");
        scheduler.setPoolSize(10);
        scheduler.setErrorHandler(new ErrorHandler() {
            @Override
            public void handleError(Throwable throwable) {
                LOGGER.error("Thread[{}] occur error:{}", Thread.currentThread().getName(), throwable.getMessage());
            }
        });
        return scheduler;
    }

    @Bean
    public LocalScriptTestDriveService localScriptTestDriveService() {
        return new LocalScriptTestDriveService();
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //  单个数据大小
        factory.setMaxFileSize("100MB");
        /// 总上传数据大小
        factory.setMaxRequestSize("200MB");
        return factory.createMultipartConfig();
    }
}
