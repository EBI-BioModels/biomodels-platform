package net.biomodels.jummp.core.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.FormHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate

class RestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class)

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

    static String sendPost(final String requestURL, JSONObject jsonObject) {
        HttpPost request = new HttpPost(requestURL)
        StringEntity params = new StringEntity(jsonObject.toString(), "UTF-8")
        request.addHeader("Content-Type", "application/json")
        request.setEntity(params)

        CloseableHttpClient httpClient = HttpClientBuilder.create().build()
        CloseableHttpResponse response = httpClient.execute(request)
        String output = ""
        try {
            org.apache.http.HttpEntity entity = response.getEntity()
            if (entity != null) {
                output = EntityUtils.toString(entity)
            }
        } catch (Exception ignored) {
            LOGGER.error("An exception occurred when creating a new Deep Learning model because of ${ignored.message}.")
        } finally {
            response.close()
        }

        output
    }
}
