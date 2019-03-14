# Run apps on Kubernetes with Spring Cloud Kubernetes

This project shows how to use
[Spring Cloud Kubernetes](https://spring.io/projects/spring-cloud-kubernetes)
to run a Java app on Kubernetes. Thanks to the abstraction provided by Spring,
the app source code does not have any dependencies on Kubernetes API:
Spring Cloud Kubernetes takes care of integrating the app with Kubernetes features,
such as service discovery and configuration using `ConfigMap` objects.
Spring Cloud Kubernetes also provides support for client-side load balancing
and circuit breaker patterns.

<img src="https://i.imgur.com/ONL4XgA.png" alt="App screenshot"/>

Source code does not depend on any Kubernetes library, just plain
Spring stuff. See for example the backend source code:
```java
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableDiscoveryClient
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
```

## How to use it?

This repository contains 2 apps: a [backend](backend) and a [frontend](frontend).

Note there is no `Dockerfile` available in this repository.
[Cloud Native Buildpacks](https://buildpacks.io) are used to
build and package each app into a fully secured & optimized
Docker image,

Run this command to create a Docker image for each app:
```bash
$ pack build alexandreroman/hello-spring-k8s-backend \
  --path backend --builder=nebhale/java-build
$ pack build alexandreroman/hello-spring-k8s-frontend \
  --path frontend --builder=nebhale/java-build
```

You can see how these Docker images are created:
 - a JDK is provided for compilation
 - Maven is downloaded, as well as all project dependencies
   (about half the Internet 😉)
 - source code is compiled and packaged with a JRE
 - app artifact is optimized for runtime execution
   (no root user, dependencies flattened in a directory,
   different image layers for caching purpose, etc.)

## Deploy to a Kubernetes cluster

Run this command to deploy these Docker images to your cluster:
```bash
$ kubectl apply -f k8s
namespace/hello-spring-k8s created
configmap/backend created
deployment.apps/backend created
deployment.apps/frontend created
service/frontend-lb created
service/backend created
clusterrole.rbac.authorization.k8s.io/service-discovery-client created
rolebinding.rbac.authorization.k8s.io/default:service-discovery-client created
```

Check the `LoadBalancer` allocated IP address to get the public app endpoint:
```bash
$ kubectl -n hello-spring-k8s get svc frontend-lb
NAME          TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)        AGE
frontend-lb   LoadBalancer   10.97.52.148   X.X.X.X       80:32686/TCP   110s
```

Scale out the backend app, and see service discovery and client-side load balancing
in action:
```bash
$ kubectl -n hello-spring-k8s scale --replicas=3 deployment backend
deployment.extensions/backend scaled
$ kubectl -n hello-spring-k8s get pods
NAME                       READY   STATUS    RESTARTS   AGE
backend-9f8b55656-7nvxf    0/1     Running   0          18s
backend-9f8b55656-nxlfv    0/1     Running   0          18s
backend-9f8b55656-w5f4t    1/1     Running   0          4m34s
frontend-8757b7848-sghcb   1/1     Running   0          4m34s
```

Edit the backend configuration using a `ConfigMap` object, and
check out how the new configuration is applied:
```bash
$ kubectl -n hello-spring-k8s edit configmap backend
configmap/backend edited
```

## Contribute

Contributions are always welcome!

Feel free to open issues & send PR.

## License

Copyright &copy; 2019 [Pivotal Software, Inc](https://pivotal.io).

This project is licensed under the [Apache Software License version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
