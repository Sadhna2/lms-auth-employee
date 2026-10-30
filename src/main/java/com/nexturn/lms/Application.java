package com.nexturn.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the scoped backend and discovers its controllers, services and repositories. */
@SpringBootApplication
public class Application {
    /** Runs the same application locally with the embedded HTTP server. */
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
