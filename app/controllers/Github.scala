package controllers

import play.api.libs.json.*
import play.api.mvc.*

import lila.app.*
import lila.core.msg.SystemMsg

final class Github(env: Env) extends LilaController(env):

  def secretScanning = Action.async(checkSignature): req =>
    req.body.asOpt[List[lila.oauth.AccessTokenApi.GithubSecretScan]] match
      case Some(scans) =>
        env.oAuth.tokenApi
          .secretScanning(scans)
          .flatMap:
            _.sequentially: (token, url) =>
              val text = lila.msg.MsgPreset.apiTokenRevoked(url)
              env.msg.api.systemPost(SystemMsg.mustRead(token.userId, text))
        NoContent
      case None => badRequest("JSON does not match expected format")

  private val checkSignature: BodyParser[JsValue] = parse.using: req =>
    parse.raw.validateM: raw =>
      val body = raw.asBytes().map(_.utf8String)
      val identifier = req.headers.get("Github-Public-Key-Identifier")
      val signature = req.headers.get("Github-Public-Key-Signature")

      (body, identifier, signature)
        .mapN: (b, i, s) =>
          env.web.github
            .verify(b, s, i)
            .map:
              if _ then Json.parse(b).asRight
              else badRequest("Signature verification failed").asLeft
        .fold(fuccess(badRequest("Missing headers for signature verification").asLeft))(identity)

  private def badRequest(msg: String): Result = BadRequest(jsonError(msg))
