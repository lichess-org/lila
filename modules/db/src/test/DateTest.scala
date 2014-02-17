package lila.db

import api._
import Implicits._
import org.joda.time.DateTime
import org.specs2.execute.{ Result, AsResult }
import org.specs2.mutable._
import org.specs2.specification._
import play.api.libs.json._
import play.api.test._
import reactivemongo.api._
import reactivemongo.bson._

class DateTest extends Specification with WithColl {

  case class TestEvent(id: String, on: DateTime)

  import JsTube.Helpers._
  implicit val eTube: JsTube[TestEvent] = JsTube[TestEvent](
    (__.json update readDate('on)) andThen Json.reads[TestEvent],
    Json.writes[TestEvent] andThen (__.json update writeDate('on))
  )

  val date = DateTime.now
  import play.modules.reactivemongo.json.ImplicitBSONHandlers._

  "date conversion" should {
    "js to bson" in {
      val doc = JsObjectWriter.write(Json.obj(
        "ca" -> $gt($date(date))
      ))
      doc.getAsTry[BSONDocument]("ca") flatMap { gt =>
        gt.getAsTry[BSONDateTime]("$gt")
      } must_== scala.util.Success(BSONDateTime(date.getMillis))
    }
  }

  "save and retrieve" should {

    sequential

    "tube" in {
      val event = TestEvent("test2", date)
      withColl { coll =>
        implicit val tube = eTube inColl coll
        ($remove($select.all) >>
          $insert(event) >>
          $find.one($select.all)).await must beSome.like {
            case TestEvent("test2", d) => d must_== date
          }
      }
    }
    "$set DateTime" in {
      $set("foo" -> $date(date)) must_== Json.obj(
        "$set" -> Json.obj("foo" -> Json.obj("$date" -> date.getMillis)))
    }
  }
}
