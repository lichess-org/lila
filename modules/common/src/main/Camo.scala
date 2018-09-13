package lila.common

final class Camo(endpoint: String, secret: String) {
  def apply(url: String): String = {
    if (secret == "") url
    else {
      val mac = javax.crypto.Mac.getInstance("HMACSHA1")
      mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes, "HMACSHA1"))
      val signature = java.util.Base64.getUrlEncoder.encodeToString(mac.doFinal(url.getBytes))
      val target = java.util.Base64.getUrlEncoder.encodeToString(url.getBytes)
      s"""$endpoint/$signature/$target"""
    }
  }
}
