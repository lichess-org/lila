package lila.user

import lila.db.Implicits._
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
    "find user" in new WithDb {
      def repo = Env.current.userRepo
      repo.find(repo.query byId "thibault" limit 10).await must haveSize(1)
    }
    // "convert user to mongo" in new WithApplication {
    //   (Users.json toMongo user) map (_ \ "createdAt" \ "$date") must beLike {
    //     case JsSuccess(JsNumber(millis), _) ⇒ millis.toInt must be_>=(2000)
    //   }
    // }
    // "find thibault" in new WithApplication {
    //   (repo.find byId "thibault").await must beSome
    // }
  }
}
