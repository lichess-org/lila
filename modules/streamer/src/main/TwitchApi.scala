package lila.streamer

import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.Secret

final private class TwitchApi(ws: StandaloneWSClient, config: TwitchConfig)(using Executor):

  import Stream.Twitch
  import Twitch.given

  private var tmpToken = Secret("init")

  def fetchStreams(
      streamers: List[Streamer],
      page: Int,
      pagination: Option[Twitch.Pagination]
  ): Fu[List[Twitch.TwitchStream]] =
    (config.clientId.nonEmpty && config.secret.value.nonEmpty && page < 10).so:
      val query = List(
        "game_id" -> "743", // chess
        "first"   -> "100"  // max results per page
      ) ::: List(
        pagination.flatMap(_.cursor).map { "after" -> _ }
      ).flatten
      ws.url(config.endpoint)
        .withQueryStringParameters(query*)
        .withHttpHeaders(
          "Client-ID"     -> config.clientId,
          "Authorization" -> s"Bearer ${tmpToken.value}"
        )
        .get()
        .flatMap:
          case res if res.status == 200 =>
            res.body[JsValue].validate[Twitch.Result] match
              case JsSuccess(result, _) => fuccess(result)
              case JsError(err)         => fufail(s"twitch $err ${lila.log.http(res.status, res.body)}")
          case res if res.status == 401 && res.body.contains("Invalid OAuth token") =>
            logger.warn("Renewing twitch API token")
            renewToken >> fuccess(Twitch.Result(None, None))
          case res => fufail(s"twitch ${lila.log.http(res.status, res.body)}")
        .recover { case e: Exception =>
          logger.warn(e.getMessage)
          Twitch.Result(None, None)
        }
        .monSuccess(_.tv.streamer.twitch)
        .flatMap { result =>
          if result.data.exists(_.nonEmpty) then
            fetchStreams(streamers, page + 1, result.pagination).map(result.liveStreams ::: _)
          else fuccess(Nil)
        }

  private def renewToken: Funit =
    ws.url("https://id.twitch.tv/oauth2/token")
      .withQueryStringParameters(
        "client_id"     -> config.clientId,
        "client_secret" -> config.secret.value,
        "grant_type"    -> "client_credentials"
      )
      .post(Map.empty[String, String])
      .flatMap:
        case res if res.status == 200 =>
          res.body[JsValue].asOpt[JsObject].flatMap(_.str("access_token")) match
            case Some(token) =>
              tmpToken = Secret(token)
              funit
            case _ => fufail(s"twitch.renewToken ${lila.log.http(res.status, res.body)}")
        case res => fufail(s"twitch.renewToken ${lila.log.http(res.status, res.body)}")
