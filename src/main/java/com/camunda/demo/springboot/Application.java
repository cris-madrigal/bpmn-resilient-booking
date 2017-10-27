package com.camunda.demo.springboot;

import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class Application {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  public static void main(String... args) {
    SpringApplication.run(Application.class, args);
    ProcessEngine engine = BpmPlatform.getDefaultProcessEngine();
    log.debug("ProcessEngine initiated {}",engine);

    //UserGenerator.addUser(engine,"demo","demo","demo", "user");
  }



}
