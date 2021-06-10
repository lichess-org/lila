package lila.irc

import play.api.libs.json._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config.Secret
import lila.memo.RateLimit

final private class DiscordClient(ws: StandaloneWSClient, url: Secret)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val limiter = new RateLimit[DiscordMessage](
    credits = 1,
    duration = 15 minutes,
    key = "discord.client"
  )

  def apply(channel: Double)(content: String): Funit = apply(DiscordMessage(channel, content))

  def webhook = apply(DiscordClient.channels.webhook) _
  def comms   = apply(DiscordClient.channels.comms) _

  def apply(msg: DiscordMessage): Funit =
    limiter(msg) {
      if (url.value.isEmpty) fuccess(lila.log("discord").info(msg.toString))
      else
        ws.url(url.value)
          .post(
            Json
              .obj(
                "content" -> msg.content,
                "channel" -> msg.channel
              )
              .noNull
          )
          .flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[discord] $url $msg ${res.status} ${res.body}")
          }
          .nevermind
    }(funit)
}

private case class DiscordMessage(
    channel: Double,
    content: String
) {

  override def toString = s"[$channel] $content"
}

private object DiscordClient {

  object channels {
    val webhook = 672938635120869400d
    val comms   = 685084348096970770d
  }
}
