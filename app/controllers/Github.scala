package controllers

import lila.core.net.Bearer
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }

final class Github(env: Env) extends LilaController(env):

  private val logger = lila.log("github")

  def secretScanning = AnonBodyOf(parse.json): body =>
    body
      .asOpt[List[JsObject]]
      .map:
        _.flatMap: obj =>
          for
            token <- (obj \ "token").asOpt[String]
            url   <- (obj \ "url").asOpt[String]
          yield Bearer(token) -> url
        .toMap
      .foreach: tokensMap =>
        env.oAuth.tokenApi
          .test(tokensMap.keys.toList)
          .map:
            _.map: (bearer, token) =>
              token match
                case Some(token) =>
                  logger.info(s"revoking token ${token.plain} for user ${token.userId}")
                  env.oAuth.tokenApi.revoke(token.plain)
                  tokensMap
                    .get(bearer)
                    .map: url =>
                      env.msg.api.systemPost(
                        token.userId,
                        lila.msg.MsgPreset.apiTokenRevoked(url)
                      )
                case None =>
                  logger.info(s"ignoring token $bearer")

    NoContent
