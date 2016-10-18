package lila.mod

import play.api.libs.json._

import chess.format.FEN
import lila.common.PimpedJson._
import lila.evaluation._
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(assessApi: AssessApi) {

  def apply(user: User): Fu[Option[JsObject]] =
    assessApi.getPlayerAggregateAssessmentWithGames(user.id) flatMap {
      _ ?? {
        case PlayerAggregateAssessment.WithGames(pag, games) =>
          GameRepo withInitialFens games map { gamesWithFen =>
            Json.obj(
              "assessment" -> pag,
              "games" -> JsObject(gamesWithFen.map { g =>
                g._1.id -> gameWithFenWrites.writes(g)
              })
            ).some
          }
      }
    }

  import lila.user.JsonView.modWrites

  private implicit val playerFlagsWrites = OWrites[PlayerFlags] { f =>
    Json.obj(
      "ser" -> f.suspiciousErrorRate.option(true),
      "aha" -> f.alwaysHasAdvantage.option(true),
      "hbr" -> f.highBlurRate.option(true),
      "mbr" -> f.moderateBlurRate.option(true),
      "cmt" -> f.consistentMoveTimes.option(true),
      "nfm" -> f.noFastMoves.option(true),
      "sha" -> f.suspiciousHoldAlert.option(true)
    ).noNull
  }
  private implicit val gameAssWrites = Writes[GameAssessment] { a =>
    JsNumber(a.id)
  }
  private implicit val playerAssWrites = Json.writes[PlayerAssessment]
  private implicit val playerAggAssWrites = Json.writes[PlayerAggregateAssessment]

  private implicit val gameWithFenWrites = Writes[(Game, Option[FEN])] {
    case (g, fen) => Json.obj(
      "initialFen" -> fen.map(_.value),
      // "createdAt" -> g.createdAt.getDate,
      "pgn" -> g.pgnMoves.mkString(" ")
    ).noNull
  }
}
