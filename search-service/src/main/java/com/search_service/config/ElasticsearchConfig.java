package com.search_service.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestClient elasticsearchRestClient(ElasticsearchProperties properties) {
        return RestClient.builder(HttpHost.create(properties.uri())).build();
    }

    @Bean(destroyMethod = "close")
    public ElasticsearchClient elasticsearchClient(RestClient elasticsearchRestClient) {
        RestClientTransport transport = new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
