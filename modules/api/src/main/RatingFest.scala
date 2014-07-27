package lila.api

import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.Implicits._
import lila.game.Game.gameBSONHandler
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

    val oldUserColl = db("user4")
    val oldUserRepo = new UserRepo {
      def userTube = lila.user.tube.userTube inColl oldUserColl
    }
    val gameColl = lila.game.tube.gameTube.coll
    // val limit = Int.MaxValue
    // val limit = 100000
    val limit = 500000

    def rerate(g: Game) = UserRepo.pair(
      g.whitePlayer.userId,
      g.blackPlayer.userId
    ).flatMap {
        case (Some(white), Some(black)) =>
          def ratingLens(u: User) = PerfPicker.mainOrDefault(g)(u.perfs).intRating
          GameRepo.setRatingDiffs(g.id, ratingLens(white), ratingLens(black)) zip
            perfsUpdater.save(g, white, black) void
        case _ => funit
      }

    def unrate(game: Game) =
      (game.whitePlayer.ratingDiff.isDefined || game.blackPlayer.ratingDiff.isDefined) ?? GameRepo.unrate(game.id).void

    var nb = 0
    println("Removing history")
    db("history3").remove(BSONDocument()) >> {
      println("Gathering cheater IDs")
      oldUserRepo.engineIds flatMap { engineIds =>
        lila.game.tube.gameTube |> { implicit gameTube =>
          val query = $query(lila.game.Query.rated)
            // .batch(100)
            .sort($sort asc G.createdAt)
          var started = nowMillis
          println("Starting the party")
          $enumerate.over[Game](query, limit) { game =>
            nb = nb + 1
            if (nb % 1000 == 0) {
              val perS = 1000 * 1000 / math.max(1, nowMillis - started)
              started = nowMillis
              println("Processed %d games at %d/s".format(nb, perS))
            }
            game.userIds match {
              case _ if !game.finished => funit
              case _ if game.fromPosition => funit
              case List(uidW, uidB) if (uidW == uidB) => funit
              case List(uidW, uidB) if engineIds(uidW) || engineIds(uidB) => unrate(game)
              case List(uidW, uidB) => rerate(game)
              case _ => funit
            }
          } andThen { case _ => println(nb) }
        }
      }
    }
  }
}
