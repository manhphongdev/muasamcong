package com.muasamcong.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.codelibs.jcifs.smb.DialectVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.smb.session.SmbSessionFactory;

@Configuration
@ConfigurationProperties(prefix = "smb")
@Getter
@Setter
@Slf4j
public class SmbConfig {

    private String host;
    private int port;
    private String domain;
    private String username;
    private String password;
    private String shareFolder;

    @Bean
    public SmbSessionFactory smbSessionFactory() {
        SmbSessionFactory factory = new SmbSessionFactory();

        factory.setHost(host);
        factory.setPort(port);

        factory.setDomain("");

        factory.setUsername(username);
        factory.setPassword(password);

        factory.setShareAndDir(shareFolder);

        factory.setSmbMinVersion(DialectVersion.SMB210);
        factory.setSmbMaxVersion(DialectVersion.SMB311);

        return factory;
    }
}
