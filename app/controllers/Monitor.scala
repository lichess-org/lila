package controllers

import play.api.libs.ws.WS
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.duration._

import lila.app._

object Monitor extends LilaController {

  private val url = "http://api.monitor.lichess.org/render"
  private object path {
    val coachPageView = "servers.lichess.statsite.counts.main.counter.coach.page_view.profile"
  }
  private val coachPageViewCache = Env.memo.asyncCache.multi[lila.user.User.ID, Result](
    name = "monitor.coachPageView",
    f = userId =>
    Env.coach.api byId lila.coach.Coach.Id(userId) flatMap {
      _ ?? { coach =>
        val days = coach.daysOld atLeast 3 atMost 30
        WS.url(url).withQueryString(
          "format" -> "json",
          "target" -> s"""summarize(${path.coachPageView}.$userId,"1d","sum",false)""",
          // "target" -> s"""summarize(servers.lichess.statsite.counts.main.counter.insight.request,'1d','sum',false)""",
          "from" -> s"-${days}d",
          "until" -> "now"
        ).get() map {
            case res if res.status == 200 => Ok(res.body)
            case res =>
              lila.log("monitor").warn(s"coach ${res.status} ${res.body}")
              NotFound
          }
      }
    },
    expireAfter = _.ExpireAfterWrite(10 minute))

  def coachPageView = Secure(_.Coach) { ctx => me =>
    coachPageViewCache get me.id
  }
}
