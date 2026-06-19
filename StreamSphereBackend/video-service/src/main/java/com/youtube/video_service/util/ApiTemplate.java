package com.youtube.video_service.util;

import java.util.Map;

public interface ApiTemplate {

    // apiUrl + endpoint + query params are used to build the final URI.
    public Object makeGetCall(String apiUrl, String endPoint, Map<String, String> queryParams);

    // public Object makePostCall(String apiUrl, String endPoint, Map<String, String> queryParams, Object requestBody, String token);

    public Object makePostCall(String apiUrl, String endPoint, Map<String, String> queryParams, Object requestBody);

    public Object makePutCall(String apiUrl, String endPoint, Map<String, String> queryParams, Object requestBody);
}
