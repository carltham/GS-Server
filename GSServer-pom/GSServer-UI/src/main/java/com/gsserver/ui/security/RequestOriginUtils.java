package com.gsserver.ui.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Determines whether a request genuinely originates from the local host.
 *
 * <p>A request is treated as local only when its real connection address is loopback AND no proxy
 * header reveals a non-loopback hop. Behind nginx/Cloudflare the connection address is loopback
 * (nginx runs on the box), but {@code X-Forwarded-For}/{@code X-Real-IP} carry the real client
 * (Cloudflare) IP, so proxied public requests are correctly rejected. A remote client hitting the
 * backend directly cannot forge a loopback connection address, so it is rejected too.
 */
public final class RequestOriginUtils {

  private RequestOriginUtils() {}

  public static boolean isLoopbackRequest(HttpServletRequest request) {
    if (request == null || !isLoopback(request.getRemoteAddr())) {
      return false;
    }

    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      for (String hop : forwardedFor.split(",")) {
        if (!isLoopback(hop.trim())) {
          return false;
        }
      }
    }

    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank() && !isLoopback(realIp.trim())) {
      return false;
    }

    return true;
  }

  private static boolean isLoopback(String address) {
    if (address == null || address.isBlank()) {
      return false;
    }
    try {
      return InetAddress.getByName(address).isLoopbackAddress();
    } catch (UnknownHostException exception) {
      return false;
    }
  }
}
