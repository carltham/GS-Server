package com.gsserver.ui.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

  // Angular client-side routes that must serve the SPA shell on a direct load/refresh.
  @GetMapping({"/hardening", "/proxy", "/proxy/install", "/users"})
  public String forward() {
    return "forward:/";
  }
}