package org.paismart.pai_test.Config;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class EsConfig {

    @Value("${spring.elasticsearch.uris}")
    private String esUri;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Bean(destroyMethod = "close")
    public Rest5Client rest5Client() {
        String auth = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        return Rest5Client.builder(URI.create(esUri))
                .setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "Basic " + auth)
                })
                .build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(Rest5Client rest5Client) {
        return new Rest5ClientTransport(
                rest5Client,
                new JacksonJsonpMapper()
        );
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
