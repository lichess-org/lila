package controllers

import scala.concurrent.duration._
import play.api.libs.ws.WS
import play.api.mvc._, Results._
import play.api.Play.current

import lila.app._
import views._

object Monitor extends LilaController {

  private val url = "http://api.monitor.lichess.org/render"
  private object path {
    val coachPageView = "servers.lichess.statsite.counts.main.counter.coach.page_view.profile"
  }
  private val coachPageViewCache = lila.memo.AsyncCache[lila.user.User.ID, Result](
    f = userId => WS.url(url).withQueryString(
      "format" -> "json",
      "target" -> s"""summarize(${path.coachPageView}.$userId,"1d","sum",false)""",
      // "target" -> s"""summarize(servers.lichess.statsite.counts.main.counter.insight.request,'1d','sum',false)""",
      "from" -> "-7d",
      "until" -> "now"
    ).get() map {
        case res if res.status == 200 => Ok(res.body)
        case res =>
          lila.log("monitor").warn(s"coach ${res.status} ${res.body}")
          NotFound
      },
    timeToLive = 10 seconds
  )

  def coachPageView = Secure(_.Coach) { ctx =>
    me =>
      coachPageViewCache(me.id)
  }
}
