package lila.irc

import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config.Secret
import lila.memo.RateLimit

final private class SlackClient(ws: StandaloneWSClient, url: Secret)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val defaultChannel = "tavern"

  private val limiter = new RateLimit[SlackMessage](
    credits = 1,
    duration = 15 minutes,
    key = "slack.client"
  )

  def apply(msg: SlackMessage): Funit =
    limiter(msg) {
      if (url.value.isEmpty) fuccess(lila.log("slack").info(msg.toString))
      else
        ws.url(url.value)
          .post(
            Json
              .obj(
                "username"   -> msg.username,
                "text"       -> msg.text,
                "icon_emoji" -> s":${msg.icon}:",
                "channel"    -> (msg.channel != defaultChannel).option(s"#${msg.channel}")
              )
              .noNull
          )
          .flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[slack] $url $msg ${res.status} ${res.body}")
          }
          .nevermind
    }(funit)
}

private object SlackClient {

  private[irc] object rooms {
    val general          = "team"
    val tavern           = "tavern"
    val tavernBots       = "tavern-bots"
    val tavernNotes      = "tavern-notes"
    val tavernAppeal     = "tavern-appeal"
    val tavernLog        = "tavern-log"
    val broadcast        = "broadcast"
    val tavernMonitorAll = "tavern-monitor-all"
    val gdprLog          = "gdpr-log"
  }

  private[irc] object stage {
    val name = "stage.lichess.org"
    val icon = "volcano"
  }
}

private case class SlackMessage(
    username: String,
    text: String,
    icon: String,
    channel: String
) {

  override def toString = s"[$channel] :$icon: @$username: $text"
}
