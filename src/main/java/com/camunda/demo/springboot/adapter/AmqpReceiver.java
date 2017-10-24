package com.camunda.demo.springboot.adapter;

import java.util.UUID;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.camunda.demo.springboot.ProcessConstants;

@Component
@Profile("!test")
public class AmqpReceiver {

  @Autowired
  private ProcessEngine camunda;

  public AmqpReceiver() {
  }
  
  public AmqpReceiver(ProcessEngine camunda) {
    this.camunda = camunda;
  }
 
  @RabbitListener(bindings = @QueueBinding( //
      value = @Queue(value = "anfrage_queue", durable = "true"), //
      exchange = @Exchange(value = "anfrage", type = "topic", durable = "true"), //
      key = "create"))
  @Transactional  
  public void anfrageEingegangen(String payload) {
    handleMessage(payload, "AnfrageEingegangen");
  }

  // Probably shouldn't be one generic one for everybody
  public ProcessInstance handleMessage(String payload, String messageName) {
    return camunda.getRuntimeService().createMessageCorrelation(messageName) //
       //Optional: Set variables .setVariable(ProcessConstants.VAR_NAME_shipmentId, shipmentId) //
        .correlateWithResult() //
        .getProcessInstance();
  }
  
  @RabbitListener(bindings = @QueueBinding( //
      value = @Queue(value = "pdf_response", durable = "true"), //
      exchange = @Exchange(value = "pdf", type = "topic", durable = "true"), //
      key = "created"))
  @Transactional  
  public void pdfErzeugt(String payload) {
    handleMessage(payload, "PdfErzeugt");
  }
  
 
  
}
