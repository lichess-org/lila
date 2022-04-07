package lila.irc

import play.api.libs.json._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.WSAuthScheme
import scala.concurrent.duration._

import lila.common.config.Secret
import lila.common.String.urlencode

final private class ZulipClient(ws: StandaloneWSClient, config: ZulipClient.Config)(implicit
    ec: scala.concurrent.ExecutionContext
) {
  private val dedupMsg = lila.memo.OnceEvery.hashCode[ZulipMessage](15 minutes)

  def apply(stream: ZulipClient.stream.Selector, topic: String)(content: String): Funit = {
    apply(stream(ZulipClient.stream), topic)(content)
    funit // don't wait for zulip
  }

  def apply(stream: String, topic: String)(content: String): Funit = {
    send(ZulipMessage(stream = stream, topic = topic, content = content))
    funit // don't wait for zulip
  }

  def sendAndGetLink(stream: ZulipClient.stream.Selector, topic: String)(
      content: String
  ): Fu[Option[String]] = sendAndGetLink(stream(ZulipClient.stream), topic)(content)

  def sendAndGetLink(stream: String, topic: String)(
      content: String
  ): Fu[Option[String]] = {
    send(ZulipMessage(stream = stream, topic = topic, content = content)).map {
      // Can be `None` if the message was rate-limited (i.e Someone already created a conv a few minutes earlier)
      _.map { msgId =>
        s"https://${config.domain}/#narrow/stream/${urlencode(stream)}/topic/${urlencode(topic)}/near/$msgId"
      }
    }
  }

  private def send(msg: ZulipMessage): Fu[Option[ZulipMessage.ID]] = dedupMsg(msg) ?? {
    if (config.domain.isEmpty) fuccess(lila.log("zulip").info(msg.toString)) inject None
    else
      ws
        .url(s"https://${config.domain}/api/v1/messages")
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
          case res if res.status == 200 =>
            (res.body[JsValue] \ "id").validate[ZulipMessage.ID] match {
              case JsSuccess(result, _) => fuccess(result.some)
              case JsError(err)         => fufail(s"[zulip]: $err, $msg ${res.status} ${res.body}")
            }
          case res => fufail(s"[zulip] $msg ${res.status} ${res.body}")
        }
        .monSuccess(_.irc.zulip.say(msg.stream))
        .logFailure(lila.log("zulip"))
        .recoverDefault

  }
}

private object ZulipClient {

  case class Config(domain: String, user: String, pass: Secret)
  import io.methvin.play.autoconfig._
  implicit val zulipConfigLoader = AutoConfig.loader[Config]

  object stream {
    object mod {
      val log          = "mod-log"
      val adminLog     = "mod-admin-log"
      val adminGeneral = "mod-admin-general"
      val commsPublic  = "mod-comms-public"
      val commsPrivate = "mod-comms-private"
      val hunterCheat  = "mod-hunter-cheat"
      val hunterBoost  = "mod-hunter-boost"
      val adminAppeal  = "mod-admin-appeal"
      val usernames    = "mod-usernames"
      def adminMonitor(tpe: IrcApi.ModDomain) = tpe match {
        case IrcApi.ModDomain.Comm                           => "mod-admin-monitor-comm"
        case IrcApi.ModDomain.Cheat | IrcApi.ModDomain.Boost => "mod-admin-monitor-hunt"
        case _                                               => "mod-admin-monitor-other"
      }
    }
    val general   = "general"
    val broadcast = "content-broadcast"
    val blog      = "content-blog"
    type Selector = ZulipClient.stream.type => String
  }
}

private case class ZulipMessage(
    stream: String,
    topic: String,
    content: String
) {

  override def toString = s"[$stream:$topic] $content"
}

private object ZulipMessage {
  type ID = Int
}
