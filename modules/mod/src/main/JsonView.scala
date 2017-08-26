package lila.mod

import play.api.libs.json._
import play.api.libs.json.JodaWrites._

import chess.format.FEN
import lila.common.PimpedJson._
import lila.evaluation._
import lila.game.JsonView.blursWriter
import lila.game.{ Game, GameRepo }
import lila.user.User

final class JsonView(
    assessApi: AssessApi,
    relationApi: lila.relation.RelationApi,
    userJson: lila.user.JsonView
) {

  import lila.user.JsonView.modWrites

  private implicit val playerFlagsWrites = OWrites[PlayerFlags] { f =>
    Json.obj()
      .add("ser" -> f.suspiciousErrorRate)
      .add("aha" -> f.alwaysHasAdvantage)
      .add("hbr" -> f.highBlurRate)
      .add("mbr" -> f.moderateBlurRate)
      .add("cmt" -> f.consistentMoveTimes)
      .add("nfm" -> f.noFastMoves)
      .add("sha" -> f.suspiciousHoldAlert)
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
      "emts" -> g.clockHistory.isDefined ?? g.moveTimes.map(_.map(_.centis)),
      "blurs" -> Json.obj(
        "white" -> g.whitePlayer.blurs,
        "black" -> g.blackPlayer.blurs
      )
    ).noNull
  }

  def apply(user: User): Fu[Option[JsObject]] =
    assessApi.getPlayerAggregateAssessmentWithGames(user.id) flatMap {
      _ ?? {
        case PlayerAggregateAssessment.WithGames(pag, games) => for {
          gamesWithFen <- GameRepo withInitialFens games.filter(_.clockHistory.isDefined)
          moreGames <- GameRepo.extraGamesForIrwin(user.id, 25) map {
            _.filter { g => !games.exists(_.id == g.id) } take 20
          }
          moreGamesWithFen <- GameRepo withInitialFens moreGames
          allGamesWithFen = gamesWithFen ::: moreGamesWithFen
        } yield Json.obj(
          "user" -> userJson(user),
          "assessment" -> pag,
          "games" -> JsObject(allGamesWithFen.map { g =>
            g._1.id -> {
              gameWithFenWrites.writes(g) ++ Json.obj(
                "color" -> g._1.player(user).map(_.color.name)
              )
            }
          })
        ).some
      }
    }
}

object JsonView {

  private[mod] implicit val modlogWrites: Writes[Modlog] = Json.writes[Modlog]
}
