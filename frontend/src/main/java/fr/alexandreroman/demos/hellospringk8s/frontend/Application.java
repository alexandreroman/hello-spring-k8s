/*
 * Copyright (c) 2020 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.demos.hellospringk8s.frontend;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        // Create a client-side load balanced REST client.
        return new RestTemplate();
    }
}

@Controller
@Slf4j
@RequiredArgsConstructor
class IndexController {
    private final BackendClientService client;

    @GetMapping("/")
    public String index(Model model) {
        log.info("Calling backend");
        final HelloResponse result = client.greetings();
        log.info("Received result from backend: {}", result);

        model.addAttribute("message", result.getMessage());
        model.addAttribute("source", result.getSource());

        return "index";
    }
}

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class HelloResponse {
    private String message;
    private String source;
}

@FeignClient(name = "hello-spring-k8s-backend",
        url = "${services.backend}",
        fallback = BackendClientServiceFallback.class)
interface BackendClientService {
    @GetMapping("/")
    HelloResponse greetings();
}

@Lazy
@Component
@Slf4j
class BackendClientServiceFallback implements BackendClientService {
    @Override
    public HelloResponse greetings() {
        log.warn("Failed to call backend service: using fallback response");

        final HelloResponse resp = new HelloResponse();
        resp.setMessage("Your backend is in another castle");
        resp.setSource("fallback implementation");
        return resp;
    }
}
