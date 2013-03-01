package lila.user

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.Await

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
  lazy val db = {
    val _db = connection("lichess")
    // Await.ready(_db.drop, timeout)
    _db
  }
}

class RepoTest extends Specification {

  import Common._

  val repo = new UserRepo(db, "user2")

  "The user repo" should {
    "find users" in {
      Await.result(repo.find(repo.query.q, 2), timeout) must haveSize(2)
    }
    // "find thibault" in {
    //   Await.result(repo byId "thibault", timeout) must beSome
    // }
  }
}
