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
import lila.user.{ User, UserRepo }

object GlickoMigration {

  def apply(
    db: lila.db.Env,
    gameEnv: lila.game.Env,
    userEnv: lila.user.Env) = {

    val oldUserColl = db("user3")
    val repo = UserRepo

    val enumerator: Enumerator[BSONDocument] = lila.game.tube.gameTube |> { implicit gameTube ⇒
      val query = $query(lila.game.Query.rated)
        .projection(BSONDocument(G.playerUids -> true))
        .sort($sort asc G.createdAt)
      query.cursor[BSONDocument].enumerate(1000, true)
    }

    fuccess("done")
  }
}
