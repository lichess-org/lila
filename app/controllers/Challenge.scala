package controllers

import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Result, Results, Call, RequestHeader, Accepting }
import scala.concurrent.duration._

import lila.api.{ Context, BodyContext }
import lila.app._
import lila.challenge.{ Challenge => ChallengeModel }
import lila.common.{ HTTPRequest, LilaCookie }
import views.html

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

  private[controllers] def reach(id: String)(implicit ctx: Context): Fu[Result] =
    env.api byId id flatMap {
      case None                                 => notFound
      case Some(challenge) if isMine(challenge) => Ok(html.challenge.mine(challenge)).fuccess
      case Some(challenge)                      => Ok(html.challenge.theirs(challenge)).fuccess
    }

  private def isMine(challenge: ChallengeModel)(implicit ctx: Context) = challenge.challenger match {
    case Left(anon)  => HTTPRequest sid ctx.req contains anon.secret
    case Right(user) => ctx.userId contains user.id
  }

  def cancel(id: String) = Open { implicit ctx =>
    OptionFuResult(env.api byId id) { challenge =>
      if (isMine(challenge)) env.api remove challenge inject Redirect(routes.Lobby.home)
      else notFound
    }
  }
}
