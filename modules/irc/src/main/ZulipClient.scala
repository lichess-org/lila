package lila.irc

import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.WSAuthScheme
import scala.concurrent.duration._

import lila.common.config.Secret
import lila.memo.RateLimit

final private class ZulipClient(ws: StandaloneWSClient, config: ZulipClient.Config)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private val limiter = new RateLimit[ZulipMessage](
    credits = 1,
    duration = 15 minutes,
    key = "zulip.client"
  )

  def apply(stream: String = ZulipClient.stream.default, topic: String = ZulipClient.topic.default)(
      content: String
  ): Funit =
    apply(ZulipMessage(stream = stream, topic = topic, content = content))

  def mod(topic: String = ZulipClient.topic.default) = apply(stream = ZulipClient.stream.mod, topic = topic) _

  def apply(msg: ZulipMessage): Funit =
    limiter(msg) {
      if (config.domain.isEmpty) fuccess(lila.log("zulip").info(msg.toString))
      else
        ws.url(s"https://${config.domain}/api/v1/messages")
          .withAuth(config.user, config.pass.value, WSAuthScheme.BASIC)
          .post(
            Map(
              "type"    -> "stream",
              "to"      -> msg.stream,
              "topic"   -> msg.topic,
              "content" -> msg.content
            )
          )
          .flatMap {
            case res if res.status == 200 => funit
            case res                      => fufail(s"[zulip] $msg ${res.status} ${res.body}")
          }
          .nevermind
    }(funit)
}

private object ZulipClient {

  case class Config(domain: String, user: String, pass: Secret)
  import io.methvin.play.autoconfig._
  implicit val zulipConfigLoader = AutoConfig.loader[Config]

  object stream {
    val lila    = "lila"
    val mod     = "lila-mod"
    val default = lila
  }
  object topic {
    val general       = "general"
    val notes         = "notes"
    val clientReports = "clientReports"
    val default       = general
  }
}

private case class ZulipMessage(
    stream: String,
    topic: String,
    content: String
) {

  override def toString = s"[$stream] $content"
}
