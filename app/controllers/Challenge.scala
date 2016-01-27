package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }
import scala.concurrent.duration._

import lila.app._
import lila.common.{ HTTPRequest, LilaCookie }

object Challenge extends LilaController {

  private def env = Env.challenge

  private val PostRateLimit = new lila.memo.RateLimit(5, 1 minute)

  def all = Auth { implicit ctx =>
    me =>
      env.api.findByDestId(me.id) zip
      env.api.findByChallengerId(me.id) map {
        case (out, in) => Ok(env.jsonView.all(in, out)) as JSON
      }
  }
}
