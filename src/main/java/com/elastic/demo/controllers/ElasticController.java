package com.elastic.demo.controllers;

import com.elastic.demo.model.AirportCreateDto;
import com.elastic.demo.model.Airport;
import com.elastic.demo.service.ElasticsearchService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "index")
@Tag(name = "Elastic search controller", description = "endpoints for elastic search operations.")
public class ElasticController {
//    private Log4j logger;
    private ElasticsearchService elasticsearchService;

    @Autowired
    public ElasticController(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @GetMapping("test")
    public String testController() {
        return "api working...";
    }

    @GetMapping("create")
    @Operation(summary = "create an index", description = "creates an index in the elastic search cluster")
    @ApiResponse(responseCode = "201", description = "create index")
    public ResponseEntity<String> createIndex() {
        elasticsearchService.createIndex("airports");
        return ResponseEntity.status(HttpStatus.CREATED).body("created index successfully");
    }

    @PostMapping("upload")
    @Operation(summary = "create single", description = "creates a single document to elastic search")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "created document in elastic search"),
            @ApiResponse(responseCode = "500", description = "error while creating document")
    })
    public ResponseEntity<?> uploadSingle(@RequestBody AirportCreateDto airportDto) {
        try {
            Airport a = elasticsearchService.singleUpload(airportDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(a);
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("failed to upload document: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Bulk-upload airports CSV",
            description = """
            Accepts a CSV file, parses it into `Airport` beans and
            performs a bulk upload into the `airports` index.
            The CSV columns must map to the fields of the `Airport` class.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload succeeded",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Bad CSV or parse error",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
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

    @Operation(
            summary = "Search airports",
            description = """
            Performs a text search on the `airports` index.
            The query must be of the form `field:value`, e.g. `name:Heathrow`.
            """
    )
    @Parameters({
            @Parameter(name = "query",
                    description = "Search expression in the format `field:value`",
                    example = "country:India",
                    required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Airport.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid query",
                    content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("search")
    public ResponseEntity<?> searchAirports(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
            ) {
        try {
//            System.out.println(query);
            List<Airport> result = elasticsearchService.searchByText(query, page, size);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch data");
        }
    }

    @GetMapping("airports")
    public ResponseEntity<Object> fetchAllRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            List<Airport> res = elasticsearchService.fetchAll(page, size);
            return ResponseEntity.status(HttpStatus.OK).body(res);
        } catch (IOException e) {
//            throw new RuntimeException(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch data");
        }
    }

    @GetMapping("aggregate")
    public ResponseEntity<?> fetchAllRecords(
        @RequestParam() String agg
    ) {
        try {
            Map<String, Object> res = elasticsearchService.aggregateRecords(agg);
            return ResponseEntity.status(HttpStatus.OK).body(res);
        } catch (Exception e) {
//            throw new RuntimeException(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch data");
        }
    }
}


