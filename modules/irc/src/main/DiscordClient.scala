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

  def apply(msg: DiscordMessage): Funit =
    limiter(msg) {
      if (url.value.isEmpty) fuccess(lila.log("discord").info(msg.toString))
      else
        ws.url(url.value)
          .post(
            Json
              .obj(
                "content" -> msg.text,
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
