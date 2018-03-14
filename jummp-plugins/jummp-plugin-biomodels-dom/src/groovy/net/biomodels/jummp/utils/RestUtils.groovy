package net.biomodels.jummp.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class RestUtils {

    public static RestTemplate restTemplate;
    private static ObjectMapper objectMapper = new ObjectMapper()

    static RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate()
        }
        return restTemplate
    }

    static <T> T exchange(URI uri, HttpMethod httpMethod, TypeReference<T> tClass) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<String> responseEntity = getRestTemplate().exchange(uri, httpMethod, entity, String.class)
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return objectMapper.readValue(responseEntity.getBody(), tClass)
        }
        return null
    }
}
