package com.elastic.demo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.analysis.EdgeNGramTokenFilter;
import co.elastic.clients.elasticsearch._types.analysis.StemmerTokenFilter;
import co.elastic.clients.elasticsearch._types.analysis.TokenFilterDefinition;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import com.elastic.demo.models.Airport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ElasticsearchService {

    private final ElasticsearchClient client;

    @Autowired
    public ElasticsearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public void bulkUpload(List<Airport> airports, String indexName) throws IOException {
        int batchSize = 1000;
        int total = airports.size();

        for(int i=0; i <= total; i += batchSize) {
            int end = Math.min(i+batchSize, total);

            List<Airport> batch = airports.subList(i, end);
            if(batch.isEmpty()) continue;
            System.out.println("batch size bef sending: "+batch.size());
//            List<BulkOperation> operations = new ArrayList<>();

            BulkRequest.Builder br = getBuilder(indexName, batch);
            BulkResponse response = client.bulk(br.build());

            if(response.errors()) {
                System.err.println("errors occurred in batch from: " + i + " to " + (end-1));
            } else {
                System.out.println("successfully inserted batch from: " + i + " to " + (end-1));
            }
        }
    }

    private static BulkRequest.Builder getBuilder(String indexName, List<Airport> batch) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        System.out.println("batch size get builder: "+batch.size());
        for(Airport a: batch) {
            System.out.println("code airport" + a.getCode());
            br.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(a.getCode())
                            .document(a)));
//                operations.add(BulkOperation.of(b -> b
//                        .index(idx -> idx
//                                .index(indexName)
//                                .document(a)
//                        )
//                    )
//                );

        }
        return br;
    }

    public void createIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index("airports")).value();

            if(exists) {
                System.out.println("index already exists");
                return;
            }

            CreateIndexResponse response = client.indices().create(cl -> cl
                    .index("airports"));
        } catch (IOException e) {
            throw new IllegalStateException("failed to create index", e);
        }
//                        .settings(s -> s
//                                .analysis(an -> an
//                                        .analyzer("custom_analyzer", a -> a
//                                                .custom(c -> c
//                                                        .filter(Arrays.asList("lowercase", "custom_filter"))
//                                                        .tokenizer("standard"))
//                                        )
////                                        .filter("custom_filter", f -> f
////                                                .definition(new StemmerTokenFilter.Builder()
////                                                        .language("english")
////                                                        .build()
////                                                )
////                                        )
////                                        .filter("custom_filter", TokenFilterDefinition.of(tf ->
//                                                tf.stemmer(s -> s.language("english"))))
//                .mappings(m -> m
//                        .properties("description", createTextField(false))
//                        .properties("code", new Property.Builder().text(t -> t.fields("raw", f -> f.keyword(k -> k))));
//                        .properties("keywords", createKeywordField())
//                        .properties("price", createPriceField())
//                        .properties("created_at", createDateField())
//                        .properties("location", createGeoPointField()))
//                        .build();
//                .settings(s -> s
//                        .analysis(a -> a
//                                .filter("autocomplete_filter", f
//                                        .type("edge_ngram")
//                                        .put("minGram", 2)
//                                        .put("maxGram", 20))
//                                .filter("autocomplete_filter", f -> f
//                                        .edgeNGram(ng -> ng
//                                                .minGram(1)
//                                                .maxGram(20)
//                                        )
//                                )
//
//                        )
//                )
//        );
    }
}
