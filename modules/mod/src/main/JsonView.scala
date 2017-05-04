package lila.mod

import play.api.libs.json._

import chess.format.FEN
import lila.common.PimpedJson._
import lila.evaluation._
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(
    assessApi: AssessApi,
    relationApi: lila.relation.RelationApi,
    userJson: lila.user.JsonView
) {

  def apply(user: User): Fu[Option[JsObject]] =
    assessApi.getPlayerAggregateAssessmentWithGames(user.id) flatMap {
      _ ?? {
        case PlayerAggregateAssessment.WithGames(pag, games) => for {
          gamesWithFen <- GameRepo withInitialFens games
        } yield Json.obj(
          "user" -> userJson(user),
          "assessment" -> pag,
          "games" -> JsObject(gamesWithFen.map { g =>
            g._1.id -> {
              gameWithFenWrites.writes(g) ++ Json.obj(
                "color" -> g._1.player(user).map(_.color.name)
              )
            }
          })
        ).some
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

  private implicit val gameWithFenWrites = OWrites[(Game, Option[FEN])] {
    case (g, fen) => Json.obj(
      "initialFen" -> fen.map(_.value),
      // "createdAt" -> g.createdAt.getDate,
      "pgn" -> g.pgnMoves.mkString(" "),
      "variant" -> g.variant.exotic.option(g.variant.key),
      "emts" -> g.clockHistory.isDefined ?? g.moveTimes.map(_.map(_.centis))
    ).noNull
  }
}

object JsonView {

  private[mod] implicit val modlogWrites: Writes[Modlog] = Json.writes[Modlog]
}
