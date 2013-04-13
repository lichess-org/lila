package lila.user

import tube.userTube
import lila.db.Implicits._
import lila.db.api._
import lila.db.test.WithDb

import org.specs2.mutable.Specification

import play.api.test._
import play.api.libs.json._

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

final class RepoTest extends Specification {

  val user = User(
    id = "thibault",
    username = "Thibault",
    elo = 1200,
    nbGames = 10,
    nbRatedGames = 5,
    nbWins = 5,
    nbLosses = 5,
    nbDraws = 0,
    nbWinsH = 5,
    nbLossesH = 2,
    nbDrawsH = 0,
    nbAi = 3,
    isChatBan = false,
    enabled = true,
    roles = Nil,
    settings = Map.empty,
    bio = None,
    engine = false,
    toints = 0,
    createdAt = DateTime.now)

  import makeTimeout.large

  def cleanup {
    $remove($select.all) >> $insert(user) await 
  }

  "The user repo" should {
    // "idempotency" in new WithDb {
    //   lazy val repo = cleanRepo
    //   repo.find.byId(user.id) await timeout must_== user.some
    // }
    "date selector" in {
      "include" in new WithDb {
        cleanup
        $find.one(Json.obj(
          "ca" -> $gt($date(user.createdAt - RichInt(1).hours))
        )).await must_== user.some
      }
    }
  }
}
