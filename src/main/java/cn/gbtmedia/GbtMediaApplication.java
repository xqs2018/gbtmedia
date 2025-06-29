package cn.gbtmedia;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

/**
 * @author xqs
 */
@Slf4j
@SpringBootApplication
public class GbtMediaApplication {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        SpringApplication.run(GbtMediaApplication.class, args);
        log.info("application gbtMedia started in {} seconds",(System.currentTimeMillis() - start)/1000d);
        Environment env = SpringUtil.getApplicationContext().getEnvironment();
        String ip = ServerConfig.getInstance().getPublicIp();
        String port = env.getProperty("server.port");
        String path = env.getProperty("server.servlet.context-path");
        log.info("\n----------------------------------------------------------\n\t" +
                "Application Gbtmedia is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + path + "\n\t" +
                "External: \thttp://" + ip + ":" + port + path + "\n\t" +
                "Swagger: \thttp://" + ip + ":" + port + path + "swagger-ui/index.html\n" +
                "----------------------------------------------------------");
    }

}
