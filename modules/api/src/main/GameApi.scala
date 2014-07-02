package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.{ AnalysisRepo, Analysis }
import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.Game.{ BSONFields => G }
import lila.game.tube.gameTube
import lila.game.{ Game, PgnDump }
import lila.hub.actorApi.{ router => R }
import makeTimeout.short

private[api] final class GameApi(
    makeUrl: Any => Fu[String],
    apiToken: String,
    pgnDump: PgnDump) {

  private def makeNb(token: Option[String], nb: Option[Int]) =
    math.min(check(token) ? 200 | 10, nb | 10)

  def list(
    username: Option[String],
    rated: Option[Boolean],
    analysed: Option[Boolean],
    withAnalysis: Boolean,
    token: Option[String],
    nb: Option[Int]): Fu[JsObject] = $find($query(Json.obj(
    G.status -> $gte(chess.Status.Mate.id),
    G.playerUids -> username,
    G.rated -> rated.map(_.fold(JsBoolean(true), $exists(false))),
    G.analysed -> analysed.map(_.fold(JsBoolean(true), $exists(false)))
  ).noNull) sort lila.game.Query.sortCreated, makeNb(token, nb)) flatMap
    gamesJson(withAnalysis, token) map { games =>
      Json.obj("list" -> games)
    }

  def one(
    id: String,
    withAnalysis: Boolean,
    token: Option[String]): Fu[Option[JsObject]] =
    $find byId id map (_.toList) flatMap gamesJson(withAnalysis, token) map (_.headOption)

  private def gamesJson(withAnalysis: Boolean, token: Option[String])(games: List[Game]): Fu[List[JsObject]] =
    AnalysisRepo doneByIds games.map(_.id) flatMap { analysisOptions =>
      (games map { g => withAnalysis ?? (pgnDump(g) map (_.some)) }).sequenceFu flatMap { pgns =>
        (games map { g => makeUrl(R.Watcher(g.id, g.firstPlayer.color.name)) }).sequenceFu map { urls =>
          val validToken = check(token)
          games zip urls zip analysisOptions zip pgns map {
            case (((g, url), analysisOption), pgnOption) =>
              gameToJson(g, url, analysisOption, pgnOption,
                withAnalysis = withAnalysis,
                withBlurs = validToken,
                withHold = validToken,
                withMoveTimes = validToken)
          }
        }
      }
    }

  private def check(token: Option[String]) = token ?? (apiToken==)

  private def gameToJson(
    g: Game,
    url: String,
    analysisOption: Option[Analysis],
    pgnOption: Option[Pgn],
    withAnalysis: Boolean,
    withBlurs: Boolean = false,
    withHold: Boolean = false,
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
        "moveTimes" -> withMoveTimes.fold(
          g.moveTimes.zipWithIndex.filter(_._2 % 2 == i).map(_._1),
          JsNull),
        "blurs" -> withBlurs.option(p.blurs),
        "hold" -> p.holdAlert.ifTrue(withHold).fold[JsValue](JsNull) { h =>
          Json.obj(
            "ply" -> h.ply,
            "mean" -> h.mean,
            "sd" -> h.sd
          )
        },
        "analysis" -> analysisOption.map(_.summary).flatMap(_.find(_._1 == p.color).map(_._2)).map(s =>
          JsObject(s map {
            case (nag, nb) => nag.toString.toLowerCase -> JsNumber(nb)
          })
        )
      ).noNull
    }),
    "analysis" -> analysisOption.ifTrue(withAnalysis).|@|(pgnOption).apply {
      case (analysis, pgn) => JsArray(analysis.infoAdvices zip pgn.moves map {
        case ((info, adviceOption), move) => Json.obj(
          "move" -> move.san,
          "eval" -> info.score.map(_.centipawns),
          "mate" -> info.mate,
          "variation" -> info.variation.isEmpty.fold(JsNull, info.variation mkString " ")
        ).noNull
      })
    },
    "winner" -> g.winnerColor.map(_.name),
    "url" -> url
  ).noNull
}
