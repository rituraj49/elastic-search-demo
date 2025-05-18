package com.elastic.demo.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Airport {
    @CsvBindByName(column = "code")
    private String code;

    @CsvBindByName(column = "icao")
    private String icao;

    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "latitude")
    private String latitude;

    @CsvBindByName(column = "longitude")
    private String longitude;

    @CsvBindByName(column = "elevation")
    private int elevation;

    @CsvBindByName(column = "url")
    private String url;

    @CsvBindByName(column = "time_zone")
    private String time_zone;

    @CsvBindByName(column = "city_code")
    private String city_code;

    @CsvBindByName(column = "country_code")
    private String country_code;

    @CsvBindByName(column = "city")
    private String city;

    @CsvBindByName(column = "state")
    private String state;

    @CsvBindByName(column = "county")
    private String county;

    @CsvBindByName(column = "type")
    private String type;

}
