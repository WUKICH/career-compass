package com.coze.aicoze.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.lke.v20231130.LkeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TencentLkeConfig {

    @Value("${tencent.lke.secret-id:}")
    private String secretId;
    @Value("${tencent.lke.secret-key:}")
    private String secretKey;
    @Value("${tencent.lke.region:ap-guangzhou}")
    private String region;

    @Bean
    public LkeClient lkeClient() {
        Credential credential = new Credential(secretId, secretKey);
        ClientProfile profile = new ClientProfile();
        log.info("Init Tencent LKE client, region={}", region);
        return new LkeClient(credential, region, profile);
    }
}
