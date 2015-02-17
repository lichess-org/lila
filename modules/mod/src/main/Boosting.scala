package lila.mod

import lila.db.Types.Coll
import lila.game.Game
import lila.user.User
import chess.variant.Variant
import chess.Color

import reactivemongo.bson._
import scala.concurrent._


final class BoostingApi(modApi: ModApi, collBoosting: Coll, nbGamesToMark: Int) {
  import BoostingApi._

  private implicit val boostingRecordBSONHandler = Macros.handler[BoostingRecord]

  def getBoostingRecord(id: String): Fu[Option[BoostingRecord]] =
    collBoosting.find(BSONDocument("_id" -> id))
      .one[BoostingRecord]

  def createBoostRecord(record: BoostingRecord) =
    collBoosting.update(BSONDocument("_id" -> record.id), record, upsert = true).void

  def determineBoosting(record: BoostingRecord, winner: User): Funit = {
    if (record.games >= nbGamesToMark) {
      modApi.autoBooster(winner.username)
    } else {
      funit
    }
  }

  def boostingId(winner: User, loser: User): String = winner.id + "/" + loser.id

  def check(game: Game, whiteUser: User, blackUser: User): Funit = {
    if (game.rated 
      && game.accountable 
      && game.playedTurns <= 10 
      && !game.isTournament 
      && game.winnerColor.isDefined
      && game.variant == Variant.default
      && !game.isCorrespondence
      && game.clock.fold(false){_.limitInMinutes >= 1}) {
      game.winnerColor match {
        case Some(a) => {
          val result: GameResult = a match { 
            case Color.White => GameResult(winner = whiteUser, loser = blackUser)
            case Color.Black => GameResult(winner = blackUser, loser = whiteUser)
          }
          val id = boostingId(result.winner, result.loser)
          getBoostingRecord(id).flatMap{
            case Some(record) =>
              val newRecord = BoostingRecord(
                _id = id,
                player = result.winner.id,
                games = record.games + 1
                )
              createBoostRecord(newRecord) >> determineBoosting(newRecord, result.winner)
            case none => createBoostRecord(BoostingRecord(
                _id = id,
                player = result.winner.id,
                games = 1
              ))
          }
        }
        case none => funit
      }
    } else {
      funit
    }
  }

}

object BoostingApi {

  case class BoostingRecord(
    _id: String,
    player: String,
    games: Int) {
    def id = _id
  }

  case class GameResult(
    winner: User,
    loser: User)

}
