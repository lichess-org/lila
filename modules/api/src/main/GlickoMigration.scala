package lila.api

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import org.goochjs.glicko2._
import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.Implicits._
import lila.game.Game
import lila.game.Game.{ BSONFields ⇒ G }
import lila.round.PerfsUpdater.{ Ratings, resultOf, updateRatings, mkPerf, system, makeProgress }
import lila.user.{ User, UserRepo, HistoryRepo, Glicko, GlickoEngine, Perfs, Perf, HistoryEntry }

object GlickoMigration {

  def apply(
    db: lila.db.Env,
    gameEnv: lila.game.Env,
    userEnv: lila.user.Env) = {

    val oldUserColl = db("user3")
    val oldUserRepo = new UserRepo {
      def userTube = lila.user.tube.userTube inColl oldUserColl
    }
    val gameColl = lila.game.tube.gameTube.coll
    val limit = Int.MaxValue
    // val limit = 100000
    // val limit = 1000
    var nb = 0

    import scala.collection.mutable
    val ratings = mutable.Map.empty[String, Ratings]
    val histories = mutable.Map.empty[String, mutable.ListBuffer[HistoryEntry]]

    val enumerator: Enumerator[Option[Game]] = lila.game.tube.gameTube |> { implicit gameTube ⇒
      import Game.gameBSONHandler
      $query(lila.game.Query.rated)
        // .batch(1000)
        .sort($sort asc G.createdAt)
        .cursor[Option[Game]].enumerate(limit, false)
    }

    def iteratee(isEngine: Set[String]): Iteratee[Option[Game], Unit] = {
      Iteratee.foreach[Option[Game]] {
        _ foreach { game ⇒
          nb = nb + 1
          if (nb % 1000 == 0) println(nb)
          game.userIds match {
            case _ if !game.finished                                   ⇒
            case _ if game.source.exists(lila.game.Source.Position ==) ⇒ unrate(game)
            case List(uidW, uidB) if (uidW == uidB)                    ⇒ unrate(game)
            case List(uidW, uidB) if isEngine(uidW) || isEngine(uidB)  ⇒ unrate(game)
            case List(uidW, uidB) ⇒ {
              val ratingsW = ratings.getOrElseUpdate(uidW, mkRatings)
              val ratingsB = ratings.getOrElseUpdate(uidB, mkRatings)
              val prevRatingW = ratingsW.global.getRating.toInt
              val prevRatingB = ratingsB.global.getRating.toInt
              val result = resultOf(game)
              updateRatings(ratingsW.global, ratingsB.global, result, system)
              updateRatings(ratingsW.white, ratingsB.black, result, system)
              game.variant match {
                case chess.Variant.Standard ⇒
                  updateRatings(ratingsW.standard, ratingsB.standard, result, system)
                case chess.Variant.Chess960 ⇒
                  updateRatings(ratingsW.chess960, ratingsB.chess960, result, system)
                case _ ⇒
              }
              chess.Speed(game.clock) match {
                case chess.Speed.Bullet ⇒
                  updateRatings(ratingsW.bullet, ratingsB.bullet, result, system)
                case chess.Speed.Blitz ⇒
                  updateRatings(ratingsW.blitz, ratingsB.blitz, result, system)
                case chess.Speed.Slow | chess.Speed.Unlimited ⇒
                  updateRatings(ratingsW.slow, ratingsB.slow, result, system)
              }
              histories.getOrElseUpdate(uidW, mkHistory) +=
                HistoryEntry(game.createdAt, ratingsW.global.getRating.toInt, ratingsW.global.getRatingDeviation.toInt, prevRatingB)
              histories.getOrElseUpdate(uidB, mkHistory) +=
                HistoryEntry(game.createdAt, ratingsB.global.getRating.toInt, ratingsB.global.getRatingDeviation.toInt, prevRatingW)
              gameColl.uncheckedUpdate(
                BSONDocument("_id" -> game.id),
                BSONDocument("$set" -> BSONDocument(
                  "p0.e" -> prevRatingW,
                  "p0.d" -> (ratingsW.global.getRating.toInt - prevRatingW),
                  "p1.e" -> prevRatingB,
                  "p1.d" -> (ratingsB.global.getRating.toInt - prevRatingB)
                )))
            }
            case _ ⇒
          }
        }
      }
    }

    def unrate(game: Game) {
      gameColl.uncheckedUpdate(
        BSONDocument("_id" -> game.id),
        BSONDocument("$unset" -> BSONDocument(
          "ra" -> true,
          "p0.d" -> true,
          "p1.d" -> true
        )))
    }

    def mkHistory = mutable.ListBuffer(
      HistoryEntry(DateTime.now, Glicko.default.intRating, Glicko.default.intDeviation, Glicko.default.intRating)
    )

    def mkRatings = {
      def r = new Rating(system.getDefaultRating, system.getDefaultRatingDeviation, system.getDefaultVolatility, 0)
      new Ratings(r, r, r, r, r, r, r, r)
    }

    def updateUsers(userPerfs: Map[String, Perfs]): Future[Unit] = lila.user.tube.userTube |> { implicit userTube ⇒
      userTube.coll.drop() flatMap { _ ⇒
        oldUserColl.genericQueryBuilder.cursor[BSONDocument].enumerate() |>>> Iteratee.foreach[BSONDocument] { user ⇒
          user.getAs[String]("_id") foreach { id ⇒
            val perfs = userPerfs get id getOrElse Perfs.default
            makeProgress(id) foreach { progress ⇒
              userTube.coll insert {
                writeDoc(user, Set("elo", "variantElos", "speedElos")) ++ BSONDocument(
                  User.BSONFields.perfs -> lila.user.Perfs.tube.handler.write(perfs),
                  User.BSONFields.rating -> perfs.global.glicko.intRating,
                  User.BSONFields.progress -> progress
                )
              }
            }
          }
        }
      }
    }

    def updateHistories(histories: Iterable[(String, Iterable[HistoryEntry])]): Funit = {
      userEnv.historyColl.drop() recover {
        case e: Exception ⇒ fuccess()
      } flatMap { _ ⇒
        Future.traverse(histories) {
          case (id, history) ⇒ HistoryRepo.set(id, history)
        }
      }
    }.void

    def mkPerfs(ratings: Ratings): Perfs = Perfs(
      global = mkPerf(ratings.global, None),
      standard = mkPerf(ratings.standard, None),
      chess960 = mkPerf(ratings.chess960, None),
      bullet = mkPerf(ratings.bullet, None),
      blitz = mkPerf(ratings.blitz, None),
      slow = mkPerf(ratings.slow, None),
      white = mkPerf(ratings.white, None),
      black = mkPerf(ratings.black, None))

    oldUserRepo.engineIds flatMap { engineIds ⇒
      (enumerator |>>> iteratee(engineIds)) flatMap { _ ⇒
        val perfs = (ratings mapValues mkPerfs).toMap
        updateHistories(histories) flatMap { _ ⇒
          updateUsers(perfs) map { _ ⇒
            println("Done!")
            "done"
          }
        }
      }
    }
  }

  private def writeDoc(doc: BSONDocument, drops: Set[String]) = BSONDocument(doc.elements collect {
    case (k, v) if !drops(k) ⇒ k -> v
  })
}
