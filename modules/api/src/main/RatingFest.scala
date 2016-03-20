package lila.api

import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.Implicits._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.Game.{ BSONFields => G }
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.round.PerfsUpdater
import lila.user.{ User, UserRepo }

object RatingFest {

  def apply(
    db: lila.db.Env,
    perfsUpdater: PerfsUpdater,
    gameEnv: lila.game.Env,
    userEnv: lila.user.Env) = {

    // val limit = Int.MaxValue
    // val limit = 100000
    val limit = Int.MaxValue
    val bulkSize = 4

    def rerate(g: Game) = UserRepo.pair(g.whitePlayer.userId, g.blackPlayer.userId).flatMap {
      case (Some(white), Some(black)) =>
        perfsUpdater.save(g, white, black, resetGameRatings = true) void
      case _ => funit
    }

    def unrate(game: Game) =
      (game.whitePlayer.ratingDiff.isDefined || game.blackPlayer.ratingDiff.isDefined) ?? GameRepo.unrate(game.id).void

    def log(x: Any) = lila.log("ratingFest") info x.toString

    var nb = 0
    for {
      _ <- fuccess(log("Removing history"))
      _ <- db("history3").remove(BSONDocument())
      _ = log("Reseting perfs")
      _ <- lila.user.tube.userTube.coll.update(
        BSONDocument(),
        BSONDocument("$unset" -> BSONDocument(
          List(
            "global", "white", "black",
            "standard", "chess960", "kingOfTheHill", "threeCheck",
            "bullet", "blitz", "classical", "correspondence"
          ).map { name => s"perfs.$name" -> BSONBoolean(true) }
        )),
        multi = true)
      _ = log("Gathering cheater IDs")
      engineIds <- UserRepo.engineIds
      _ = log(s"Found ${engineIds.size} cheaters")
      _ = log("Starting the party")
      _ <- lila.game.tube.gameTube |> { implicit gameTube =>
        val query = $query(lila.game.Query.rated)
          // val query = $query.all
          // .batch(100)
          .sort($sort asc G.createdAt)
        var started = nowMillis
        $enumerate.bulk[Game](query, bulkSize, limit) { games =>
          nb = nb + bulkSize
          if (nb % 1000 == 0) {
            val perS = 1000 * 1000 / math.max(1, nowMillis - started)
            started = nowMillis
            log("Processed %d games at %d/s".format(nb, perS))
          }
          games.map { game =>
            game.userIds match {
              case _ if !game.rated => funit
              case _ if !game.finished => funit
              case _ if game.fromPosition => funit
              case List(uidW, uidB) if (uidW == uidB) => funit
              case List(uidW, uidB) if engineIds(uidW) || engineIds(uidB) => unrate(game)
              case List(uidW, uidB) => rerate(game)
              case _ => funit
            }
          }.sequenceFu.void
        } andThen { case _ => log(nb) }
      }
    } yield ()
  }
}
