package com.elastic.demo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.transform.Settings;
import com.elastic.demo.model.Airport;
import com.elastic.demo.model.AirportCreateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticsearchService {

    private final ElasticsearchClient client;

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    @Autowired
    public ElasticsearchService(ElasticsearchClient client) {
        this.client = client;
    }

    public Airport singleUpload(AirportCreateDto data) throws IOException {
        IndexRequest<AirportCreateDto> airportReq = IndexRequest.of((id -> id
                .index("airport")
                .refresh(Refresh.WaitFor)
                .document(data)));

        IndexResponse res = client.index(airportReq);
        System.out.println(res);

        return new Airport(
                data.getCode(),
                data.getIcao(),
                data.getName(),
                data.getLatitude(),
                data.getLongitude(),
                data.getElevation(),
                data.getUrl(),
                data.getTime_zone(),
                data.getCity_code(),
                data.getCountry_code(),
                data.getCity(),
                data.getState(), data.getCounty(), data.getType()
        );
    }

    public void bulkUpload(List<Airport> airports, String indexName) throws IOException {
        int batchSize = 1000;
        int total = airports.size();

        for(int i=0; i <= total; i += batchSize) {
            int end = Math.min(i+batchSize, total);

            List<Airport> batch = airports.subList(i, end);
            if(batch.isEmpty()) continue;
//            List<BulkOperation> operations = new ArrayList<>();

            BulkRequest.Builder br = getBuilder(indexName, batch);
            BulkResponse response = client.bulk(br.build());

            if(response.errors()) {
//                System.err.println("errors occurred in batch from: " + i + " to " + (end-1));
                logger.error("errors occurred in batch from: {} to {}", i, end - 1);
            } else {
//                System.out.println("successfully inserted batch from: " + i + " to " + (end-1));
                logger.info("successfully inserted batch from: {} to {}", i, end-1);
            }
        }
    }

    private static BulkRequest.Builder getBuilder(String indexName, List<Airport> batch) {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for(Airport a: batch) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .id(a.getCode())
                            .document(a)));
        }
        return br;
    }

    public void createIndex(String indexName) {
        try {
            boolean exists = client.indices().exists(e -> e.index("airports")).value();

            if(exists) {
                logger.warn("index already exists");
                return;
            }

            CreateIndexResponse response =
                    client.indices().create(cl -> cl
                    .index(indexName)
                        .settings(s -> s
                                .analysis(an -> an
                                        .analyzer("autocomplete", a -> a
                                                .custom(c -> c
                                                        .tokenizer("whitespace")
                                                        .filter("lowercase", "autocomplete_filter")
                                                )
                                        )
                                        .filter("autocomplete_filter", f -> f
                                                .definition(df -> df
                                                        .edgeNgram(en -> en
                                                                .minGram(2)
                                                                .maxGram(20)
                                                        )
                                                )
                                        )
                                )
                        )
                        .mappings(m -> m
                            .properties("code", p -> p.keyword(k -> k))
                            .properties("icao", p -> p.keyword(k -> k))
                            .properties("name", p -> p.text(k -> k
                                    .analyzer("autocomplete")))
                            .properties("latitude", p -> p.double_(k -> k
                                    .fields("raw", t -> t.keyword(r -> r))
                            ))
                            .properties("longitude", p -> p.double_(k -> k
                                    .fields("raw", t -> t.keyword(r -> r))
                            ))
                            .properties("elevation", p -> p.integer(k -> k))
                            .properties("url", p -> p.text(k -> k))
                            .properties("time_zone", p -> p.text(k -> k))
                            .properties("city_code", p -> p.keyword(k -> k))
                            .properties("country_code", p -> p.keyword(k -> k))
                            .properties("city", p -> p.text(k -> k
                                    .analyzer("autocomplete")
                                .fields("raw", at -> at.keyword(kv -> kv))
                            ))
                            .properties("state", p -> p.text(k -> k
                                    .analyzer("autocomplete")
                                .fields("raw", at -> at.keyword(kv -> kv))
                            ))
                            .properties("county", p -> p.text(k -> k
                                    .analyzer("autocomplete")
                                .fields("raw", at -> at.keyword(kv -> kv))
                            ))
        ));
            logger.info("index {} created successfully", indexName);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new IllegalStateException("failed to create index", e);
        }
    }

    public List<Airport> fetchAll(int page, int size) throws IOException {
        int from = (page - 1) * size;
        SearchResponse<Airport> response = client.search(s -> s
                .index("airports")
                        .trackTotalHits(t -> t.enabled(true))
                .from(from)
                .size(size)
                .query(q -> q
                        .matchAll(m -> m)
                ),
                Airport.class
        );
        List<Hit<Airport>> result = response.hits().hits();
        System.out.println("result: "+ result);
        int resSize = result.size();
        System.out.println(resSize);
        List<Airport> airports = new ArrayList<Airport>();

//        long totalRecords = response.hits().total().value();
        for(Hit<Airport> hit: result) {
            airports.add((Airport) hit.source());
        }
        return airports;
    }

    public List<Airport> searchByText(String query, int page, int size) throws IOException {
        String[] queryArr = query.split(":");
        String field = queryArr[0];
        String item = queryArr[1];
        int from = (page - 1) * size;
        SearchResponse<Airport> sr = client.search(s -> s
                .index("airports")
                .from(from)
                .size(size)
                .query(q -> q
                        .match(t -> t
                                .field(field)
                                .query(item)
                        )
                ),
                Airport.class
        );

        List<Hit<Airport>> hits = sr.hits().hits();

        List<Airport> airports = new ArrayList<>();

        for(Hit<Airport> hit: hits) {
            airports.add(hit.source());
        }
        return airports;
    }

    public Map<String, Object> aggregateRecords(String aggField) throws IOException {
        SearchResponse<Void> sr = client.search(s -> s
                .index("airports")
                .size(0)
                .aggregations("country_groups", ag -> ag
                        .terms(t -> t
                                .field(aggField)
//                                .size(10)
                        )
                )
                .aggregations("avg_elevation", ag -> ag
                        .avg(avg -> avg.field("elevation"))
                )
        );

        Map<String, Aggregate> result = sr.aggregations();
        System.out.println(result);

        Map<String, Object> responseMap = new HashMap<>();

        Aggregate avgAgg = result.get("avg_elevation");

        if(avgAgg.isAvg()) {
            Double avgValue = avgAgg.avg().value();
            responseMap.put("avg_elevation", avgValue);
        }

        Aggregate countryGrouping = result.get("country_groups");

        if(countryGrouping.isSterms()) {
            List<Map<String, Object>> buckets = countryGrouping.sterms().buckets().array().stream()
                    .map(s -> {
                        Map<String, Object> map = new HashMap<>();
                                map.put("key", s.key().stringValue());
                                map.put("count", s.docCount());
                                return map;
                            })
                    .toList();
            responseMap.put("country_groups", buckets);
        }
        return responseMap;
    }
}
