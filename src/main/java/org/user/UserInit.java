package org.user;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Log4j2
@SpringBootApplication
@EntityScan(basePackages = {"org.posts.model"})
@ComponentScan(basePackages = {"org.posts.mapper", "org.user"})
public class UserInit {
    public static void main(String[] args) throws UnknownHostException {
        System.setProperty("hostName", InetAddress.getLocalHost().getHostName());
        System.setProperty("hostAddress", InetAddress.getLocalHost().getHostAddress());
        SpringApplication.run(UserInit.class, args);
    }
}