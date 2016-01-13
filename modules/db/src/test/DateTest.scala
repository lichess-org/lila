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

class DateTest extends Specification {

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
}
