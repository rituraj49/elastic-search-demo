package com.elastic.demo.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class AirportCreateDto {
    private String code;

    private String icao;

    private String name;

    private String latitude;

    private String longitude;

    private int elevation;

    private String url;

    private String time_zone;

    private String city_code;

    private String country_code;

    private String city;

    private String state;

    private String county;

    private String type;
}
