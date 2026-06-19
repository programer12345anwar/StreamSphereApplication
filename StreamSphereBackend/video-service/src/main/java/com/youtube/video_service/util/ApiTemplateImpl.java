package com.youtube.video_service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;


@Slf4j
public class ApiTemplateImpl implements ApiTemplate {

    @Autowired
    RestTemplate restTemplate;

    public String addQueryParams(String url, Map<String, String> queryParams){
        if(queryParams.size() == 0){
            return url;
        }
        int count = 1;
        url += "?";//before adding query params
        for(String key: queryParams.keySet()){
            url += key + "=" + queryParams.get(key);
            if(count < queryParams.size()){
                url += "&";
            }
            count++;
        }
        return url;
    }

    public URI createUrl(String apiUrl, String endPoint, Map<String, String> queryParams){
        String url = apiUrl + endPoint;
        url = this.addQueryParams(url, queryParams);
        return URI.create(url);
    }

    @Override
    public Object makeGetCall(String apiUrl, String endPoint, Map<String, String> queryParams) {
        // First step is to create url
        URI finalUrl = this.createUrl(apiUrl, endPoint, queryParams);
        // second step is to create request entity
        RequestEntity request = RequestEntity.get(finalUrl).build();
        // After creating request i need to hit the request
        // So, to hit request we need a library that is rest template
        ResponseEntity<Object> response = restTemplate.exchange(finalUrl, HttpMethod.GET, request, Object.class);//i will be able to catch any type of response body
        return response.getBody();
    }

    @Override
    public Object makePostCall(String apiUrl, String endPoint, Map<String, String> queryParams, Object requestBody) {
        URI url = this.createUrl(apiUrl, endPoint, queryParams);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RequestEntity<Object> request = RequestEntity
                .post(url)
                .headers(headers)
                .body(requestBody);

        ResponseEntity<Object> response = restTemplate.exchange(request, Object.class);
        log.info("Central API response status: {}", response.getStatusCode());
        if (response.getBody() == null) {
           log.warn("Central API returned a null body");
        }
        return response.getBody();
    }

    @Override
    public Object makePutCall(String apiUrl, String endPoint, Map<String, String> queryParams, Object requestBody) {
        URI url = this.createUrl(apiUrl, endPoint, queryParams);
        RequestEntity request = RequestEntity.put(url).body(requestBody);
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PUT, request, Object.class);
        return response.getBody();
    }
}
