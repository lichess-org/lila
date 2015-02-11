package lila.mod

import lila.db.Types.Coll
import lila.game.Game
import lila.user.User
import chess.Color

import reactivemongo.bson._
import scala.concurrent._


final class BoostingApi(modApi: ModApi, collBoosting: Coll) {

  private implicit val boostingRecordBSONHandler = Macros.handler[BoostingRecord]

  def getBoostingRecord(winner: User, loser: User): Fu[Option[BoostingRecord]] =
    collBoosting.find(BSONDocument("_id" -> (winner.id + "/" + loser.id)))
      .one[BoostingRecord]

  def createBoostRecord(record: BoostingRecord) =
    collBoosting.update(BSONDocument("_id" -> record._id), record, upsert = true).void

  def determineBoosting(record: BoostingRecord, winner: User): Funit = {
    if (record.games + 1 >= 3) {
      modApi.autoAdjust(winner.username)
    } else {
      funit
    }
  }

  def check(game: Game, whiteUser: User, blackUser: User): Funit = {
    if (game.rated && game.accountable && game.playedTurns <= 10 && !game.isTournament && game.winnerColor.isDefined) {
      game.winnerColor match {
        case Some(a) => {
          val result: Result = a match { 
            case Color.White => Result(winner = whiteUser, loser = blackUser)
            case Color.Black => Result(winner = blackUser, loser = whiteUser)
          }
          getBoostingRecord(result.winner, result.loser).flatMap{
            case Some(record) => 
              determineBoosting(record, result.winner) >>
              createBoostRecord(BoostingRecord(
                _id = result.winner.id + "/" + result.loser.id,
                player = result.winner.id,
                games = record.games + 1
                ))
            case none => createBoostRecord(BoostingRecord(
                _id = result.winner.id + "/" + result.loser.id,
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

case class BoostingRecord(
  _id: String,
  player: String,
  games: Int)

case class Result(
  winner: User,
  loser: User)