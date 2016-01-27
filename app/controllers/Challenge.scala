package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }
import play.api.Play.current
import scala.concurrent.duration._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.common.{ HTTPRequest, LilaCookie }
import lila.user.UserRepo
import views._

object Setup extends LilaController {

  private def env = Env.setup

  private val PostRateLimit = new lila.memo.RateLimit(5, 1 minute)

}
