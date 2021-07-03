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

  private val limiter = new RateLimit[Int](
    credits = 1,
    duration = 15 minutes,
    key = "zulip.client"
  )

  def apply(stream: ZulipClient.stream.Selector, topic: String)(
      content: String
  ): Funit =
    apply(
      ZulipMessage(stream = stream(ZulipClient.stream), topic = topic, content = content)
    )

  def apply(msg: ZulipMessage): Funit =
    limiter(msg.hashCode) {
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
    object mod {
      val log                                   = "mod-log"
      val adminLog                              = "mod-admin-log"
      val commsPrivate                          = "mod-comms-private"
      val hunterCheat                           = "mod-hunter-cheat"
      def adminMonitor(tpe: IrcApi.MonitorType) = s"mod-admin-monitor-${tpe.key}"
      def adminMonitorAll                       = "mod-admin-monitor-all"
      def adminAppeal                           = "mod-admin-appeal"
    }
    val general   = "general"
    val broadcast = "broadcast"
    type Selector = ZulipClient.stream.type => String
  }
}

private case class ZulipMessage(
    stream: String,
    topic: String,
    content: String
) {

  override def toString = s"[$stream] $content"
}
