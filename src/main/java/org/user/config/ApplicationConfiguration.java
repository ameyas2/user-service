package org.user.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Value("${external-server.hazelcast.host}")
    private String hazelcastHost;

    @Value("${external-server.hazelcast.port}")
    private String hazelcastPort;

    @Value("${external-server.hazelcast.cluster-name}")
    private String hazelcastClusterName;

    @Value("${external-server.minio.endpoint}")
    private String minioEndpoint;

    @Value("${external-server.minio.access-key}")
    private String minioAccessKey;

    @Value("${external-server.minio.secret-key}")
    private String minioSecretKey;

    @Bean
    public ClientConfig clientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(hazelcastClusterName);
        clientConfig.getNetworkConfig().addAddress(hazelcastHost + ":" + hazelcastPort);
        return clientConfig;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient
                .builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return HazelcastClient.newHazelcastClient(clientConfig());
    }
}
