package controllers

import lila.core.net.Bearer
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }

final class Github(env: Env) extends LilaController(env):

  private val logger = lila.log("github")

  def secretScanning = AnonBodyOf(parse.json): body =>
    val tokens = body
      .as[List[JsObject]]
      .map { obj =>
        val token = (obj \ "token").as[String]
        val url   = (obj \ "url").as[String]
        Bearer(token) -> url
      }

    env.oAuth.tokenApi.test(tokens.map(_._1)).map {
      _.map { (bearer, token) =>
        token match
          case Some(token) =>
            logger.info(s"revoking token ${token.plain} for user ${token.userId}")
            for
              _ <- env.oAuth.tokenApi.revoke(token.plain)
              _ <- env.msg.api.systemPost(
                token.userId,
                lila.msg.MsgPreset.apiTokenRevoked(tokens.find(_._1 == bearer).map(_._2).get)
              )
            yield ()
          case None =>
            logger.info(s"ignoring token $bearer")
      }
    }
    NoContent
