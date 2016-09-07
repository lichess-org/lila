package controllers

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

  def coachPageView = Secure(_.Coach) { ctx =>
    me =>
      WS.url(url).withQueryString(
        "format" -> "json",
        "target" -> s"""summarize(${path.coachPageView}.${me.id},"1h","sum",false)""",
        // "target" -> s"""summarize(servers.lichess.statsite.counts.main.counter.insight.request,'1d','sum',false)""",
        "from" -> "-7d",
        "until" -> "now"
      ).get() map {
          case res if res.status == 200 => Ok(res.body)
          case res =>
            lila.log("monitor").warn(s"coach ${res.status} ${res.body}")
            NotFound
        }
  }
}
