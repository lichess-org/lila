package lila.user

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

import scala.concurrent.Await
import org.joda.time.DateTime

object Common {
  import scala.concurrent._
  import scala.concurrent.duration._
  import reactivemongo.api._
  import reactivemongo.bson.handlers.DefaultBSONHandlers

  implicit val ec = ExecutionContext.Implicits.global
  implicit val writer = DefaultBSONHandlers.DefaultBSONDocumentWriter
  implicit val reader = DefaultBSONHandlers.DefaultBSONDocumentReader
  implicit val handler = DefaultBSONHandlers.DefaultBSONReaderHandler

  val timeout = 2 seconds

  lazy val connection = MongoConnection(List("localhost:27017"))
  lazy val db = connection("lichess")
}

class RepoTest extends Specification {

  import Common._

  val repo = new UserRepo(db, "user2")
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
    "find user" in {
      Await.result(repo.find(repo.query byId "thibault", 1), timeout) must haveSize(1)
    }
    "convert user to mongo" in {
      (Users.json toMongo user) map (_ \ "createdAt" \ "$date") must beLike {
        case JsSuccess(JsNumber(millis), _) ⇒ millis.toInt must be_>=(2000)
      }
    }
    "find thibault" in {
      Await.result(repo byId "thibault", timeout) must beSome
    }
  }
}
