package com.gsserver.ui.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  @GetMapping({"/hardening", "/proxy"})
  public String forward() {
    return "forward:/";
  }
}