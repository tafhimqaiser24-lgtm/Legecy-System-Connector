package com.stellar.legacy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;

@Configuration
public class StellarConfig {

    @Value("${stellar.network.horizon-url}")
    private String horizonUrl;

    @Value("${stellar.network.passphrase}")
    private String networkPassphrase;

    @Bean
    public Server stellarServer() {
        return new Server(horizonUrl);
    }

    @Bean
    public Network stellarNetwork() {
        return new Network(networkPassphrase);
    }
}
