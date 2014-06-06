package lila.api

import play.api.libs.json._

import lila.analyse.{ AnalysisRepo, Analysis }
import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.Game
import lila.game.Game.{ BSONFields => G }
import lila.game.tube.gameTube
import lila.hub.actorApi.{ router => R }
import makeTimeout.short

private[api] final class GameApi(
    makeUrl: Any => Fu[String],
    apiToken: String,
    isOnline: String => Boolean) {

  private def makeNb(nb: Option[Int]) = math.min(100, nb | 10)

  def list(
    username: Option[String],
    rated: Option[Boolean],
    analysed: Option[Boolean],
    withAnalysis: Option[Boolean],
    token: Option[String],
    nb: Option[Int]): Fu[JsObject] = $find($query(Json.obj(
    G.status -> $gte(chess.Status.Mate.id),
    G.playerUids -> username,
    G.rated -> rated.map(_.fold(JsBoolean(true), $exists(false))),
    G.analysed -> analysed.map(_.fold(JsBoolean(true), $exists(false)))
  ).noNull) sort lila.game.Query.sortCreated, makeNb(nb)) flatMap
    gamesJson(withAnalysis, token) map { games =>
      Json.obj("list" -> games)
    }

  def one(
    id: String,
    withAnalysis: Option[Boolean],
    token: Option[String]): Fu[Option[JsObject]] =
    $find byId id map (_.toList) flatMap gamesJson(withAnalysis, token) map (_.headOption)

  private def gamesJson(withAnalysis: Option[Boolean], token: Option[String])(games: List[Game]): Fu[List[JsObject]] =
    AnalysisRepo doneByIds games.map(_.id) flatMap { analysisOptions =>
      (games map { g => makeUrl(R.Watcher(g.id, g.firstPlayer.color.name)) }).sequenceFu map { urls =>
        val validToken = check(token)
        games zip urls zip analysisOptions map {
          case ((g, url), analysisOption) =>
            GameApi.gameToJson(g, url, analysisOption,
              withBlurs = validToken,
              withMoveTimes = validToken)
        }
      }
    }

  private def check(token: Option[String]) = token ?? (apiToken==)
}

private[api] object GameApi {

  def gameToJson(
    g: Game,
    url: String,
    analysisOption: Option[Analysis],
    withBlurs: Boolean = false,
    withMoveTimes: Boolean = false) = Json.obj(
    "id" -> g.id,
    "rated" -> g.rated,
    "variant" -> g.variant.name,
    "timestamp" -> g.createdAt.getDate,
    "turns" -> g.turns,
    "status" -> g.status.name.toLowerCase,
    "clock" -> g.clock.map { clock =>
      Json.obj(
        "limit" -> clock.limit,
        "increment" -> clock.increment,
        "totalTime" -> clock.estimateTotalTime
      )
    },
    "players" -> JsObject(g.players.zipWithIndex map {
      case (p, i) => p.color.name -> Json.obj(
        "userId" -> p.userId,
        "rating" -> p.rating,
        "moveTimes" -> (withMoveTimes.fold(
          g.moveTimes.zipWithIndex.filter(_._2 % 2 == i).map(_._1),
          JsNull)),
        "blurs" -> withBlurs.option(p.blurs),
        "analysis" -> analysisOption.map(_.summary).flatMap(_.find(_._1 == p.color).map(_._2)).map(s =>
          JsObject(s map {
            case (nag, nb) => nag.toString.toLowerCase -> JsNumber(nb)
          })
        )
      ).noNull
    }),
    "winner" -> g.winnerColor.map(_.name),
    "url" -> url
  ).noNull
}
