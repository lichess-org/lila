package lila.db

import org.joda.time.DateTime
import org.specs2.mutable._
import play.api.libs.json._
import play.api.test._
import Types._

class DbApiTest extends Specification {

  import api._

  val date = DateTime.now

  "operators" should {

    "$set" in {
      $set("foo" -> "bar") must_== Json.obj(
        "$set" -> Json.obj("foo" -> "bar"))
    }
    // "$set DateTime" in new WithApplication {
    //   $set("foo" -> date) must_== Json.obj(
    //     "$set" -> Json.obj("foo" -> Json.obj("$date" -> date.getMillis)))
    // }
  }
}
