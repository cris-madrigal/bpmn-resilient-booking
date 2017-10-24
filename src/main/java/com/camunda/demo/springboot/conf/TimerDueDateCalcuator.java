package com.camunda.demo.springboot.conf;

import java.util.Calendar;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component("due")
public class TimerDueDateCalcuator {

  public Date milliseconds(int millis) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MILLISECOND, millis);
    return calendar.getTime();
  }
}
