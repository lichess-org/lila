package controllers

import play.api.data.*
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest

import lila.core.net.Bearer
import lila.web.{ WebForms, StaticContent }

import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.*
import java.util.Base64

final class Github(env: Env)(using ws: StandaloneWSClient) extends LilaController(env):

  def secretScanning = Action.async(checkSignature): req =>
    req.body.asOpt[List[lila.oauth.AccessTokenApi.GithubSecretScan]] match
      case Some(scans) =>
        env.oAuth.tokenApi
          .secretScanning(scans)
          .flatMap:
            _.traverse: (token, url) =>
              env.msg.api.systemPost(token.userId, lila.msg.MsgPreset.apiTokenRevoked(url))
        NoContent
      case None => BadRequest(jsonError("JSON does not match expected format"))

  val checkSignature = parse.using { req =>
    req.pp
    req.headers.pp

    val body = parse.raw.map { raw =>
      val body = raw.asBytes().map(_.utf8String).getOrElse("")
      body // "foo"
    }

    val identifier = req.headers.get("Github-Public-Key-Identifier").getOrElse("")
    val signature  = req.headers.get("Github-Public-Key-Signature").getOrElse("")

    lila.log("github").info(s"Identifier: $identifier")
    lila.log("github").info(s"Signature: $signature")
    lila.log("github").info(s"Body: $body")

    val keys: JsObject = ???
    if !env.net.isProd && false then parse.json
    else
      keys("public_keys")
        .as[List[JsObject]]
        .find(_("key_identifier").as[String] == identifier) match
        case Some(key) =>
          val keyStr = key("key").as[String]
          keyStr.pp
          val publicKey        = getPublicKeyFromPEM(keyStr)
          val body: String = ???
          val isSignatureValid = verifySignature(body, signature, publicKey)

          lila.log("github").info(s"Signature verification result: ${isSignatureValid}")

          if isSignatureValid then parse.json
          else BadRequest(jsonError("Signature verification failed"))
        case None => BadRequest(jsonError("Public key not found"))
  }

  private lazy val githubPublicKeys = env.memo.cacheApi.unit[Option[JsValue]]:
    _.refreshAfterWrite(1.minute).buildAsyncFuture: _ =>
      ws.url("https://api.github.com/meta/public_keys/secret_scanning")
        .get()
        .map:
          case res if res.status == 200 => res.body[JsValue].some
          case _                        => none
        .recoverDefault

  def getPublicKeyFromPEM(pem: String): PublicKey =
    val publicKeyPEM = pem
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replace("-----END PUBLIC KEY-----", "")
      .replaceAll("\\s", "")
    val encoded    = Base64.getDecoder.decode(publicKeyPEM)
    val keySpec    = new X509EncodedKeySpec(encoded)
    val keyFactory = KeyFactory.getInstance("EC")
    keyFactory.generatePublic(keySpec)

  def verifySignature(body: String, signature: String, publicKey: PublicKey): Boolean =
    val sig = Signature.getInstance("SHA256withECDSA")
    sig.initVerify(publicKey)
    sig.update(body.getBytes(StandardCharsets.UTF_8))
    val signatureBytes = Base64.getDecoder.decode(signature)
    sig.verify(signatureBytes)
