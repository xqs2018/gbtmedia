package cn.gbtmedia.ftp;

import cn.gbtmedia.common.config.ServerConfig;
import cn.hutool.extra.ftp.SimpleFtpServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.impl.DefaultDataConnectionConfiguration;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xqs
 */
@Order(1)
@Slf4j
@Component
public class FtpServer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ServerConfig.Ftp config = ServerConfig.getInstance().getFtp();
        log.info("ftpServer start port {} passivePorts {}",config.getPort(),config.getPassivePorts());
        DataConnectionConfigurationFactory dataConfig = new DataConnectionConfigurationFactory();
        String accessIp = ServerConfig.getInstance().getAccessIp();
        dataConfig.setPassiveExternalAddress(accessIp);
        dataConfig.setPassivePorts(config.getPassivePorts());
        BaseUser baseUser = new BaseUser();
        baseUser.setName(config.getUsername());
        baseUser.setPassword(config.getPassword());
        baseUser.setHomeDirectory(config.getPath());
        // 设置用户权限
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        baseUser.setAuthorities(authorities);
        SimpleFtpServer ftpServer = SimpleFtpServer
                .create()
                .setPort(config.getPort())
                .addUser(baseUser);
        ftpServer.getListenerFactory().setDataConnectionConfiguration(dataConfig.createDataConnectionConfiguration());
        ftpServer.start();
    }
}
