/*
 * Copyright (c) 2019 Pivotal Software, Inc.
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

package fr.alexandreroman.demos.hellospringk8s.backend;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Data
@Configuration
@ConfigurationProperties(prefix = "backend")
class AppProperties {
    // These configuration properties are reloaded when the ConfigMap is updated.

    private String message = "Hello world!";
}

@RestController
@RequiredArgsConstructor
class HelloController {
    private final AppProperties props;
    private String hostName;

    @PostConstruct
    public void init() throws UnknownHostException {
        // Get host name once this bean is initialized.
        hostName = InetAddress.getLocalHost().getCanonicalHostName();
    }

    @GetMapping("/")
    public HelloResponse hello() {
        final HelloResponse resp = new HelloResponse();
        resp.setMessage(props.getMessage());
        resp.setSource(hostName);
        return resp;
    }
}

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class HelloResponse {
    private String message;
    private String source;
}
