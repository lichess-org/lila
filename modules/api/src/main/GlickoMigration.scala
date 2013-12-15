package lila.api

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.Implicits._
import lila.game.Game.{ BSONFields ⇒ G }
import lila.user.{ User, UserRepo, Glicko, GlickoEngine }

object GlickoMigration {

  def apply(
    db: lila.db.Env,
    gameEnv: lila.game.Env,
    userEnv: lila.user.Env) = {

    import Impl._

    val oldUserColl = db("user3")
    val limit = 1000

    val enumerator: Enumerator[BSONDocument] = lila.game.tube.gameTube |> { implicit gameTube ⇒
      val query = $query(lila.game.Query.rated)
        .projection(BSONDocument(G.playerUids -> true, G.winnerColor -> true))
        .sort($sort asc G.createdAt)
      query.cursor[BSONDocument].enumerate(limit, true)
    }

    val iteratee: Iteratee[BSONDocument, Map[String, Glicko]] =
      Iteratee.fold(Map.empty[String, Glicko]) {
        case (users, game) ⇒ uidsOf(game) match {
          case _ if users.size >= limit ⇒ users
          case None                     ⇒ users
          case Some((uidW, uidB)) ⇒ {
            val white = users get uidW getOrElse Glicko.default
            val black = users get uidB getOrElse Glicko.default
            val result = resultOf(game)
            val newWhite =
              GlickoEngine(white).calculate(black, result)
            val newBlack =
              GlickoEngine(black).calculate(white, result.negate)
            val users2 = users + (uidW -> newWhite) + (uidB -> newBlack)
            if (uidW == "controlaltdelete") {
              println("controlaltdelete")
              println(white, black)
              println(newWhite, newBlack)
              println("---")
            }
            // if (users2.size > 29) {
            //   debug(users2)
            //   throw new Exception("booo")
            // }
            users2
          }
        }
      }

    (enumerator |>>> iteratee) map { users ⇒
      debug(users)
      "done"
    }
  }

  private object Impl {

    import GlickoEngine.Result

    def debug(gs: Map[String, Glicko]) {
      println(gs.toList.sortBy(_._2.rating).map {
        case (id, glicko) ⇒ s"$id $glicko"
      } mkString "\n")
    }

    def resultOf(game: BSONDocument): Result =
      game.getAs[Boolean](G.winnerColor) match {
        case Some(true)  ⇒ Result.Win
        case Some(false) ⇒ Result.Loss
        case None        ⇒ Result.Draw
      }

    def uidsOf(game: BSONDocument): Option[(String, String)] = for {
      uids ← game.getAs[List[String]](G.playerUids)
      white ← uids.headOption filter (_.nonEmpty)
      black ← uids lift 1 filter (_.nonEmpty)
      if white != black
    } yield (white, black)
  }
}
