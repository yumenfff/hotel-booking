package com.hotelbooking.booking.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(HotelClientProperties.class)
public class HotelClientConfig {

  @Bean
  @LoadBalanced
  RestClient.Builder hotelRestClientBuilder(
      ObjectMapper objectMapper, HotelClientProperties properties) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(properties.connectTimeout())
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());
    return RestClient.builder()
        .requestFactory(requestFactory)
        .messageConverters(
            converters ->
                converters.replaceAll(
                    converter ->
                        converter instanceof MappingJackson2HttpMessageConverter
                            ? new MappingJackson2HttpMessageConverter(objectMapper)
                            : converter))
        .requestInterceptor(new BearerTokenPropagationInterceptor());
  }

  @Bean
  RestClient hotelHttpClient(
      @LoadBalanced RestClient.Builder hotelRestClientBuilder, HotelClientProperties properties) {
    return hotelRestClientBuilder.baseUrl(properties.baseUrl()).build();
  }
}
