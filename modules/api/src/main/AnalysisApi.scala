package lila.api

import chess.format.pgn.Pgn
import chess.format.UciDump
import chess.Replay
import play.api.libs.json._

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.common.PimpedJson._
import lila.db.api._
import lila.db.Implicits._
import lila.game.{ Game, GameRepo, PgnDump }
import lila.hub.actorApi.{ router => R }

private[api] final class AnalysisApi(
    apiToken: String,
    makeUrl: Any => Fu[String],
    nbAnalysis: () => Fu[Int],
    pgnDump: PgnDump) {

  private def makeNb(nb: Option[Int]) = math.min(100, nb | 10)

  private def makeSkip = nbAnalysis() map { nb => scala.util.Random.nextInt(nb) }

  def list(nb: Option[Int], token: Option[String]): Fu[JsObject] =
    if (~token != apiToken) fuccess(Json.obj("oh" -> "bummer"))
    else makeSkip flatMap { skip =>
      AnalysisRepo.skipping(skip, makeNb(nb)) flatMap { as =>
        GameRepo games as.map(_.id) flatMap { games =>
          games.map { g =>
            as find (_.id == g.id) map { _ -> g }
          }.flatten.map {
            case (a, g) => GameRepo initialFen g.id flatMap { initialFen =>
              pgnDump(g) zip makeUrl(R.Watcher(g.id, g.firstPlayer.color.name)) map {
                case (pgn, url) => (g, a, url, pgn, initialFen)
              }
            }
          }.sequenceFu map { tuples =>
            Json.obj(
              "list" -> JsArray(tuples map {
                case (game, analysis, url, pgn, fen) => Json.obj(
                  "game" -> (GameApi.gameToJson(game, url, analysis.some) ++ {
                    fen ?? { f => Json.obj("initialFen" -> f) }
                  }),
                  "analysis" -> AnalysisApi.analysisToJson(analysis, pgn),
                  "uci" -> uciMovesOf(game, fen).map(_.mkString(" "))
                ).noNull
              })
            )
          }
        }
      }
    }

  private def uciMovesOf(game: Game, initialFen: Option[String]): Option[List[String]] =
    Replay(game.pgnMoves mkString " ", initialFen, game.variant).toOption map UciDump.apply
}

private[api] object AnalysisApi {

  def analysisToJson(analysis: Analysis, pgn: Pgn) = JsArray(analysis.infoAdvices zip pgn.moves map {
    case ((info, adviceOption), move) => Json.obj(
      "ply" -> info.ply,
      "move" -> move.san,
      "eval" -> info.score.map(_.centipawns),
      "mate" -> info.mate,
      "variation" -> info.variation.isEmpty.fold(JsNull, info.variation mkString " "),
      "comment" -> adviceOption.map(_.makeComment(true, true))
    ).noNull
  })
}
