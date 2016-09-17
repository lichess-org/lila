package lila.message

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import scala.concurrent.duration._

import lila.common.LightUser
import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo, Pov }
import lila.rating.PerfType
import lila.user.User
import scala.concurrent.{ Future }
import lila.common.paginator._

final class JsonView() {

  def inbox(me: User, ignore: Paginator[Thread]): Result =
      Ok(Json.obj(
        "id" -> "hello"
      ).noNull)

}
