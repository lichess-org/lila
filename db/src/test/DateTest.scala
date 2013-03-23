package lila.db

import lila.db.test.WithTestColl
import Types._

import org.specs2.specification._
import org.specs2.mutable._
import org.specs2.execute.{ Result, AsResult }

import play.api.libs.json._
import play.api.test._
import reactivemongo.api._
import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.modules.reactivemongo.MongoJSONHelpers._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

class DateTest extends Specification {

  val date = DateTime.now
  import DbApi._

  "save and retrieve" should {

    val trans = __.json update JsonTube.Helpers.readDate('ca)

    "JsObject" in new WithTestColl {
      (coll.insert(Json.obj("ca" -> $date(date))) >>
        coll.find(Json.obj()).one[JsObject]
      ).await flatMap (x ⇒ (x transform trans).asOpt) must beSome.like {
          case o ⇒ o \ "ca" must_== date
        }
    }
    // "BSONDocument" in new WithTestColl {
    //   val doc = BSONDocument(
    //     "ca" -> BSONDateTime(date.getMillis)
    //   ).pp
    //   coll.insert(doc).map(lastError ⇒
    //     println("Mongo LastErorr:%s".format(lastError))
    //   )
    //   coll.find[JsValue](Json.obj()).one.map { person ⇒
    //     println(person)
    //   }
    //   success
    // }
    // "$set DateTime" in new WithApplication {
    //   $set("foo" -> date) must_== Json.obj(
    //     "$set" -> Json.obj("foo" -> Json.obj("$date" -> date.getMillis)))
    // }
  }
}
