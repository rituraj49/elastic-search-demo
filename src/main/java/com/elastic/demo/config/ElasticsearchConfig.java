package com.elastic.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient client() {
        System.out.println("elastic search configured");
        return ElasticsearchClient.of(b -> b
                .host("http://localhost:9200"));
    }
//    @Bean
//    public ElasticsearchProperties.Restclient restClient() {
//        return RestClient.builder(new HttpHost("localhost", 9200)).build();
//    }
//
//    @Bean
//    public ElasticsearchTransport transport(RestClient client) {
//        return new RestClientTransport(client, new JacksonJsonpMapper());
//    }
//
//    @Bean
//    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
//        return new ElasticsearchClient(transport);
//    }
}
