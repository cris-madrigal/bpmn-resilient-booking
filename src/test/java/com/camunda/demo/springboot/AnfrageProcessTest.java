package com.camunda.demo.springboot;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.camunda.bpm.scenario.run.ProcessRunner.ExecutableRunner.StartingByStarter;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.camunda.demo.springboot.adapter.AmqpReceiver;
import com.camunda.demo.springboot.conf.TestApplication;
import com.camunda.demo.springboot.rest.OrderRestController;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.NONE, //
    classes = TestApplication.class, //
    properties = { //
        "camunda.bpm.job-execution.enabled=false", //
        "camunda.bpm.auto-deployment-enabled=false", //
        "restProxyHost=api.example.org", //
        "restProxyPort=80" })
@Deployment(resources = { "anfrage.bpmn" })
@ActiveProfiles({ "test" })
public class AnfrageProcessTest {

  @Mock
  private ProcessScenario anfrageProcess;
  
  @Autowired
  private RabbitTemplate rabbitTemplate;

  // Do not use the real one to avoid RabbitMQ being connected
  private AmqpReceiver amqpReceiver;

  @Autowired
  private ProcessEngine camunda;

  @Rule
  @ClassRule
  public static ProcessEngineRule rule;

  @PostConstruct
  void initRule() {
    rule =TestCoverageProcessEngineRuleBuilder.create(camunda).build();
    // Without Coverage: new ProcessEngineRule(processEngine);
  }

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);    
    amqpReceiver = new AmqpReceiver(rule.getProcessEngine());
  }

  @Test
  public void testHappyPath() throws Exception {
    String orderId = UUID.randomUUID().toString();

    StartingByStarter starter = Scenario.run(anfrageProcess) //
      .startBy(() -> {
        // use the real thing which would also start the flow in the real system
        // as this might do other things, foremost data transformation and naming of process variables
        return amqpReceiver.handleMessage("", "AnfrageEingegangen");
      });
    
    when(anfrageProcess.waitsAtReceiveTask("ReceiveTask_WaitForPdf")).thenReturn((messageSubscription) -> {
      // again: use the real thing like receiving the real AMQP message, return a dummy shipmentId
      amqpReceiver.handleMessage("", "PdfErzeugt");
    });    
    
    // OK - everything prepared - let's go
    Scenario scenario = starter.execute();

    verify(rabbitTemplate, times(1)).convertAndSend(eq("pdf"), eq("create"), anyString());
    {
      ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
      verify(rabbitTemplate, times(1)).convertAndSend(eq("anfrage"), eq("done"), argument.capture());
      // if the body would be an object, JSON or whatever, you can easily inspect/assert it here in detail
      //assertEquals(orderId, argument.getValue());
    }

    verify(anfrageProcess).hasFinished("EndEvent_AnfrageDirektBeantwortet");
  }


  @Test
  public void testFailover() throws Exception {
    StartingByStarter starter = Scenario.run(anfrageProcess) //
      .startBy(() -> {
        // use the real thing which would also start the flow in the real system
        // as this might do other things, foremost data transformation and naming of process variables
        return amqpReceiver.handleMessage("", "AnfrageEingegangen");
      });
    

    when(anfrageProcess.waitsAtReceiveTask("ReceiveTask_WaitForPdf")).thenReturn((messageSubscription) -> {
      messageSubscription.defer("PT5S", () -> {
        // send it after some seconds - which means the response already got sent
        amqpReceiver.handleMessage("", "PdfErzeugt");
      }); 
    });    
    
    // OK - everything prepared - let's go
    Scenario scenario = starter.execute();
    
    assertThat(scenario.instance(anfrageProcess)).hasPassed("ServiceTask_SendEmail");

    // TODO: Check how to reset rabbitTemplate?
    verify(rabbitTemplate, times(1)).convertAndSend(eq("pdf"), eq("create"), anyString());
    verify(rabbitTemplate, times(1)).convertAndSend(eq("anfrage"), eq("received"), anyString());

    verify(anfrageProcess).hasFinished("EndEvent_AnfrageBearbeitet");
  }
}
