package lila.web

import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.*
import java.util.Base64

final class GitHub(cacheApi: lila.memo.CacheApi, ws: StandaloneWSClient)(using Executor):

  def verify(body: String, signature: String, identifier: String): Future[Boolean] =
    githubPublicKeys
      .get {}
      .map:
        case Some(keys) =>
          keys("public_keys")
            .as[List[JsObject]]
            .find(_("key_identifier").as[String] == identifier) match
            case Some(key) =>
              verifySignature(body, signature, getPublicKeyFromPEM(key("key").as[String]))
            case None => false
        case None => false

  private val githubPublicKeys = cacheApi.unit[Option[JsValue]]:
    _.refreshAfterWrite(1.day).buildAsyncFuture: _ =>
      ws.url("https://api.github.com/meta/public_keys/secret_scanning")
        .get()
        .map: res =>
          (res.status == 200).option(res.body[JsValue])
        .recoverDefault

  private def getPublicKeyFromPEM(pem: String): PublicKey =
    val publicKeyPEM = pem
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replaceAll("\\s", "")
    val encoded = Base64.getDecoder.decode(publicKeyPEM)
    val keySpec = new X509EncodedKeySpec(encoded)
    val keyFactory = KeyFactory.getInstance("EC")
    keyFactory.generatePublic(keySpec)

  private def verifySignature(body: String, signature: String, publicKey: PublicKey): Boolean =
    try
      val sig = Signature.getInstance("SHA256withECDSA")
      sig.initVerify(publicKey)
      sig.update(body.getBytes(StandardCharsets.UTF_8))
      val signatureBytes = Base64.getDecoder.decode(signature)
      sig.verify(signatureBytes)
    catch
      case e: Exception =>
        lila
          .log("github")
          .warn(s"Failed to verify signature: $signature, publicKey: $publicKey, body: $body", e)
        false
