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
        "game_id" -> "743", // chess-1
        "first" -> "100" // max results per page
      ) ::: List(
        pagination.flatMap(_.cursor).map { "after" -> _ }
      ).flatten
      ws.url(config.endpoint)
        .withQueryStringParameters(query*)
        .withHttpHeaders(
          "Client-ID" -> config.clientId,
          "Authorization" -> s"Bearer ${tmpToken.value}"
        )
        .get()
        .flatMap:
          case res if res.status == 200 =>
            res.body[JsValue].validate[Twitch.Result] match
              case JsSuccess(result, _) => fuccess(result)
              case JsError(err) => fufail(s"twitch $err ${lila.log.http(res.status, res.body)}")
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

  def checkStreamStatus(userId: String, keyword: Stream.Keyword): Fu[Option[TwitchStreamStatus]] =
    (config.clientId.nonEmpty && config.secret.value.nonEmpty).so:
      ensureToken.flatMap: _ =>
        ws.url("https://api.twitch.tv/helix/streams")
          .withQueryStringParameters("user_login" -> userId)
          .withHttpHeaders(
            "Client-ID" -> config.clientId,
            "Authorization" -> s"Bearer ${tmpToken.value}"
          )
          .get()
          .flatMap:
            case res if res.status == 200 =>
              res
                .body[JsValue]
                .asOpt[JsObject]
                .flatMap: obj =>
                  ((obj \ "data")
                    .asOpt[JsArray]
                    .flatMap(_.value.headOption.map(_.as[JsObject]))) match
                    case Some(streamJson) =>
                      val isLive = (streamJson \ "type").asOpt[String].contains("live")
                      val title = (streamJson \ "title").asOpt[String].getOrElse("")
                      val hasKeyword = title.toLowerCase.contains(keyword.toLowerCase)
                      val gameId = (streamJson \ "game_id").asOpt[String].getOrElse("")
                      val isChess = gameId == "743"
                      val gameName = (streamJson \ "game_name").asOpt[String]
                      fuccess(
                        TwitchStreamStatus(
                          isLive = isLive,
                          hasKeyword = hasKeyword,
                          isChess = isChess,
                          title = title.some.filter(_.nonEmpty),
                          category = gameName.filter(_.nonEmpty)
                        ).some
                      )
                    case None =>
                      fuccess(
                        TwitchStreamStatus(
                          isLive = false,
                          hasKeyword = false,
                          isChess = false,
                          title = none,
                          category = none
                        ).some
                      )
            case res if res.status == 401 && res.body.contains("Invalid OAuth token") =>
              logger.warn("Renewing twitch API token for checkStreamStatus")
              renewToken >> checkStreamStatus(userId, keyword)
            case res =>
              logger.warn(s"twitch checkStreamStatus ${lila.log.http(res.status, res.body)}")
              fuccess(none)
          .recover { case e: Exception =>
            logger.warn(s"twitch checkStreamStatus error: ${e.getMessage}")
            none
          }

  case class TwitchStreamStatus(
      isLive: Boolean,
      hasKeyword: Boolean,
      isChess: Boolean,
      title: Option[String],
      category: Option[String]
  )

  private def ensureToken: Funit =
    if tmpToken.value == "init" then renewToken else funit

  private def renewToken: Funit =
    ws.url("https://id.twitch.tv/oauth2/token")
      .withQueryStringParameters(
        "client_id" -> config.clientId,
        "client_secret" -> config.secret.value,
        "grant_type" -> "client_credentials"
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
