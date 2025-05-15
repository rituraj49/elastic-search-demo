package com.elastic.demo.controllers;

import com.elastic.demo.models.Airport;
import com.elastic.demo.service.ElasticsearchService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "index")
public class ElasticController {
    private ElasticsearchService elasticsearchService;

    @Autowired
    public ElasticController(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @GetMapping("test")
    public String testController() {
        return "api working...";
    }

    @PostMapping("create")
    public void createIndex() {
        elasticsearchService.createIndex();
    }

    @PostMapping("bulk-upload")
    public ResponseEntity<?> uploadDataset(@RequestParam("file")MultipartFile file) throws IOException {
        try(Reader reader = new InputStreamReader(file.getInputStream())) {
            CsvToBean<Airport> csvToBean = new CsvToBeanBuilder<Airport>(reader)
                    .withType(Airport.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            System.out.println("csv to bean: "+csvToBean);
            List<Airport> airports = csvToBean.parse();
            elasticsearchService.bulkUpload(airports, "airports");
        } catch (Exception e) {
            e.printStackTrace();
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to parse file: " + e.getMessage());
        }

            return ResponseEntity.status(HttpStatus.OK).body("data uploaded successfully");
    }
}
