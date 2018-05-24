package net.biomodels.jummp.core.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

class RestUtils {

    public static RestTemplate restTemplate
    private static ObjectMapper objectMapper = new ObjectMapper()

    static RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = new RestTemplate()
            List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>()
            messageConverters.add(new FormHttpMessageConverter())
            messageConverters.add(new StringHttpMessageConverter())
            restTemplate.setMessageConverters(messageConverters)
        }
        return restTemplate
    }

    static <T> T exchange(URI uri, HttpMethod method, TypeReference<T> tClass, HttpEntity<?> entity,
                          int retry = 1, ignoreError = false) {
        HttpStatusCodeException lastException = null
        for (int i = 0; i < retry; i++) {
            try {
                ResponseEntity<String> responseEntity = getRestTemplate().exchange(uri, method, entity, String.class)
                return objectMapper.readValue(responseEntity.getBody().trim(), tClass)
            } catch (HttpStatusCodeException e) {
                lastException = e
                Thread.sleep(1000)
            }
        }
        if (!ignoreError) {
            throw lastException
        } else {
            return objectMapper.readValue(lastException.getResponseBodyAsString(), tClass)
        }
    }
}
