package com.youtube.video_service.service;

import java.util.HashMap;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.youtube.video_service.dto.IsValidDTO;
import com.youtube.video_service.dto.SecurityCredential;
import com.youtube.video_service.dto.VideoDetailsDTO;
import com.youtube.video_service.util.ApiTemplate;
import lombok.extern.slf4j.Slf4j;


// Work of this class is to call central api endpoints
@Service
@Slf4j
public class CentralApiConnectionService {

    @Autowired
    ApiTemplate apiTemplate;

    @Autowired
    ModelMapper mapper;

    @Value("${central.api.url}")
    String centralApiUrl;

    // Save video details method is going to call save video details endpoint present in central
    public void saveVideoDetails( UUID channelId,
    VideoDetailsDTO videoDetailsDTO ){
         // i need to call save video details endpoint declared in your  channel controller of central api
        log.info("comes to centralConnectionApiService");
         String endPoint = "/channel/" + channelId.toString() +  "/video/upload";
         log.info("this is endpoint: "+endPoint);
         // apiurl, endpoint, queryparams, requestbody
         Object resp = apiTemplate.makePostCall(centralApiUrl, endPoint, new HashMap<>(), videoDetailsDTO);
        if (resp == null) {
            log.warn("Central API returned null response for uploading video");
        } else {
            log.info("Response over central API connection: " + resp.getClass());
        }

    }
    public boolean isValidToken(String token){
        String endPoint = "/security/validate-token/" + token;
        Object object = apiTemplate.makeGetCall(centralApiUrl,endPoint, new HashMap<>());
        IsValidDTO resp = mapper.map(object, IsValidDTO.class);
        return resp.isSuccess();
    }
    public String getCredentialFromToken(String token){
        String endPoint = "/security/get-credential/" + token;
        Object object = apiTemplate.makeGetCall(centralApiUrl, endPoint, new HashMap<>());
        SecurityCredential securityCredential = mapper.map(object, SecurityCredential.class);
        return securityCredential.getCredential();
    }

}

