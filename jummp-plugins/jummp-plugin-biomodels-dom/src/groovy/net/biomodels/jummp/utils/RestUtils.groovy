package net.biomodels.jummp.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

class RestUtils {

    public static RestTemplate restTemplate
    private static ObjectMapper objectMapper = new ObjectMapper()

    static RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate()
        }
        return restTemplate
    }

    static <T> T exchange(URI uri, HttpMethod httpMethod, TypeReference<T> tClass, int retry = 1) {
        Exception lastException = null
        for (int i = 0; i < retry; i++) {
            try {
                HttpHeaders headers = new HttpHeaders()
                headers.setContentType(MediaType.APPLICATION_JSON)
                HttpEntity<String> entity = new HttpEntity<>("parameters", headers)
                ResponseEntity<String> responseEntity = getRestTemplate().exchange(uri, httpMethod, entity, String.class)
                return objectMapper.readValue(responseEntity.getBody(), tClass)
            } catch (Exception e) {
                lastException = e
                Thread.sleep(1000)
            }
        }
        throw lastException
    }
}