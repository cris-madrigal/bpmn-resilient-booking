package com.camunda.demo.springboot.adapter;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.camunda.demo.springboot.ProcessConstants;

@Component
public class SendRequestAnsweredAdapter implements JavaDelegate {

  @Autowired
  protected RabbitTemplate rabbitTemplate;
  
  @Override
  public void execute(DelegateExecution ctx) throws Exception {
    String payload = "";
    // optional: get some data
    //String orderId = (String) ctx.getVariable(ProcessConstants.VAR_NAME_orderId);    
    
    String exchange = "anfrage";
    String routingKey = "done";
    
    rabbitTemplate.convertAndSend(exchange, routingKey, payload);
  }

}
