package lila.user

import lila.db.Implicits._
import lila.db.DbApi._
import lila.db.test.WithDb

import org.specs2.mutable._

import play.api.test._
import play.api.libs.json._

import org.joda.time.DateTime

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

  "The user repo" should {
    "idempotency" in new WithDb {
      lazy val repo = Env.current.userRepo ~ { _.remove(select.all).await }
      (repo.insert(user) >> repo.find.byId(user.id)).await must_== user.some
    }
  }
}
