package com.streamsphere.video.client.central;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.streamsphere.video.dto.response.IsValidDTO;
import com.streamsphere.video.dto.response.SecurityCredential;
import com.streamsphere.video.dto.request.VideoDetailsDTO;
import com.streamsphere.video.client.central.ApiTemplate;
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

    public void updateVideoStatus(String videoId, String status) {
        log.info("Updating video status for {} to {}", videoId, status);
        String endPoint = "/channel/video/" + videoId + "/status";
        Map<String, String> params = new HashMap<>();
        params.put("status", status);
        apiTemplate.makePutCall(centralApiUrl, endPoint, params, null);
    }

    public void updateVideoDetails(String videoId, VideoDetailsDTO videoDetailsDTO) {
        log.info("Updating video details for {}", videoId);
        String endPoint = "/channel/video/" + videoId + "/details";
        apiTemplate.makePutCall(centralApiUrl, endPoint, new HashMap<>(), videoDetailsDTO);
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



