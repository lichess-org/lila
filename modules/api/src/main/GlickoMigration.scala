package lila.api

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import org.goochjs.glicko2._
import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.Implicits._
import lila.game.Game.{ BSONFields ⇒ G }
import lila.user.{ User, UserRepo, Glicko, GlickoEngine, Perf, Perfs }

object GlickoMigration {

  def apply(
    db: lila.db.Env,
    gameEnv: lila.game.Env,
    userEnv: lila.user.Env) = {

    import Impl._

    val oldUserColl = db("user3")
    val newUserColl = lila.user.tube.userTube.coll
    val limit = 1000 // Int.MaxValue
    var nb = 0

    val enumerator: Enumerator[BSONDocument] = lila.game.tube.gameTube |> { implicit gameTube ⇒
      val query = $query(lila.game.Query.rated)
        .projection(BSONDocument(G.playerUids -> true, G.winnerColor -> true))
        .batch(1000)
        .sort($sort asc G.createdAt)
      query.cursor[BSONDocument].enumerate(limit, false)
    }

    type UserPerfs = Map[String, Perfs]
    def iteratee(isEngine: Set[String]): Iteratee[BSONDocument, UserPerfs] =
      Iteratee.fold[BSONDocument, UserPerfs](Map.empty) {
        case (perfs, game) ⇒ {
          nb = nb + 1
          if (nb % 1000 == 0) println(nb)
          uidsOf(game) match {
            case None => perfs
            case Some((uidW, uidB)) if isEngine(uidW) || isEngine(uidB) ⇒ perfs
            case Some((uidW, uidB)) ⇒ {
              val perfsW = perfs get uidW getOrElse Perfs.default
              val perfsB = perfs get uidB getOrElse Perfs.default
              val result = resultOf(game)
              val global = try {
                calculate(perfsW.global, perfsB.global, result)
              } catch {
                case e: Exception => {
                  println(e)
                  (perfsW.global, perfsB.global)
                  // throw e
                }
              }
              perfs + (uidW -> perfsW.copy(
                global = global._1
              )) + (uidB -> perfsB.copy(
                global = global._2
              ))
            }
          }
        }
      }

    def calculateStub(perfW: Perf, perfB: Perf, result: Glicko.Result): (Perf, Perf) = 
      perfW.copy(nb = perfW.nb + 1) -> perfB.copy(nb = perfB.nb + 1)

    def calculate(perfW: Perf, perfB: Perf, result: Glicko.Result): (Perf, Perf) = {
      val ratingSystem = new RatingCalculator()
      val white = rating(perfW, "white", ratingSystem)
      val black = rating(perfB, "black", ratingSystem)
      val period = new RatingPeriodResults()
      result match {
        case Glicko.Result.Draw ⇒ period.addDraw(white, black)
        case Glicko.Result.Win  ⇒ period.addResult(white, black)
        case Glicko.Result.Loss ⇒ period.addResult(black, white)
      }
      ratingSystem.updateRatings(period)
      perf(perfW, white) -> perf(perfB, black)
    }

    def rating(perf: Perf, id: String, ratingSystem: RatingCalculator) =
      new Rating(id, ratingSystem, perf.glicko.rating, perf.glicko.deviation, perf.glicko.volatility)

    def perf(old: Perf, rating: Rating) = Perf(
      Glicko(rating.getRating, rating.getRatingDeviation, rating.getVolatility),
      nb = old.nb + 1
    )

    def updateUsers(userPerfs: Map[String, Perfs]): Future[Unit] = lila.user.tube.userTube |> { implicit userTube ⇒
      userTube.coll.drop() flatMap { _ ⇒
        oldUserColl.genericQueryBuilder.cursor[BSONDocument].enumerate() |>>> Iteratee.foreach[BSONDocument] { user ⇒
          for {
            id ← user.getAs[String]("_id")
            perfs ← userPerfs get id
          } userTube.coll insert {
            user ++ BSONDocument(
              User.BSONFields.perfs -> lila.user.Perfs.tube.write(perfs)
            )
          }
        }
      }
    }

    UserRepo.engineIds flatMap { engineIds ⇒
      (enumerator |>>> iteratee(engineIds)) flatMap { perfs ⇒
        // debug(perfs)
        updateUsers(perfs) map { _ ⇒
        // fuccess {
          "done"
        }
      }
    }
  }

  private object Impl {

    def debug(gs: Map[String, Perfs]) {
      println(gs map { case (id, perfs) ⇒ s"$id $perfs" } mkString "\n")
    }

    def resultOf(game: BSONDocument): Glicko.Result =
      game.getAs[Boolean](G.winnerColor) match {
        case Some(true)  ⇒ Glicko.Result.Win
        case Some(false) ⇒ Glicko.Result.Loss
        case None        ⇒ Glicko.Result.Draw
      }

    def uidsOf(game: BSONDocument): Option[(String, String)] = for {
      uids ← game.getAs[List[String]](G.playerUids)
      white ← uids.headOption filter (_.nonEmpty)
      black ← uids lift 1 filter (_.nonEmpty)
      if white != black
    } yield (white, black)
  }
}
