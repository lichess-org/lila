package lila.mod

import shogi.{ variant, Color }
import lila.db.dsl._
import lila.game.Game
import lila.user.User

import reactivemongo.api.bson._

final class BoostingApi(
    modApi: ModApi,
    collBoosting: Coll,
    nbGamesToMark: Int,
    ratioGamesToMark: Double
)(implicit ec: scala.concurrent.ExecutionContext) {
  import BoostingApi._

  implicit private val boostingRecordBSONHandler = Macros.handler[BoostingRecord]

  private val variants = Set[variant.Variant](
    variant.Standard,
    variant.Minishogi,
    variant.Chushogi,
    variant.Annanshogi,
    variant.Kyotoshogi,
    variant.Checkshogi
  )

  def getBoostingRecord(id: String): Fu[Option[BoostingRecord]] =
    collBoosting.byId[BoostingRecord](id)

  def createBoostRecord(record: BoostingRecord) =
    collBoosting.update.one($id(record.id), record, upsert = true).void

  def determineBoosting(record: BoostingRecord, winner: User, loser: User): Funit =
    (record.games >= nbGamesToMark) ?? {
      {
        (record.games >= (winner.count.rated * ratioGamesToMark)) ?? modApi.autoBoost(winner.id, loser.id)
      } >> {
        (record.games >= (loser.count.rated * ratioGamesToMark)) ?? modApi.autoBoost(winner.id, loser.id)
      }
    }

  def boostingId(winner: User, loser: User): String = winner.id + "/" + loser.id

  def check(game: Game, senteUser: User, goteUser: User): Funit = {
    if (
      game.rated
      && game.accountable
      && game.playedPlies <= game.variant.numberOfRanks
      && !game.isTournament
      && game.winnerColor.isDefined
      && variants.contains(game.variant)
      && !game.isCorrespondence
      && game.clock.fold(false) { _.limitInMinutes >= 1 }
    ) {
      game.winnerColor match {
        case Some(a) => {
          val result: GameResult = a match {
            case Color.Sente => GameResult(winner = senteUser, loser = goteUser)
            case Color.Gote  => GameResult(winner = goteUser, loser = senteUser)
          }
          val id = boostingId(result.winner, result.loser)
          getBoostingRecord(id).flatMap {
            case Some(record) =>
              val newRecord = BoostingRecord(
                _id = id,
                games = record.games + 1
              )
              createBoostRecord(newRecord) >> determineBoosting(newRecord, result.winner, result.loser)
            case None =>
              createBoostRecord(
                BoostingRecord(
                  _id = id,
                  games = 1
                )
              )
          }
        }
        case None => funit
      }
    } else {
      funit
    }
  }

}

object BoostingApi {

  case class BoostingRecord(_id: String, games: Int) {
    def id = _id
  }

  case class GameResult(
      winner: User,
      loser: User
  )
}
