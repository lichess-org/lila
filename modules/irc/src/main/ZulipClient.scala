package lila.irc

import play.api.ConfigLoader
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.{ StandaloneWSClient, WSAuthScheme }

import lila.common.String.urlencode
import lila.core.config.Secret

final private class ZulipClient(ws: StandaloneWSClient, config: ZulipClient.Config)(using
    Executor
):
  private val dedupMsg = scalalib.cache.OnceEvery.hashCode[ZulipMessage](15.minutes)

  def apply[C: Show](stream: ZulipClient.stream.Selector, topic: String)(content: C): Funit =
    apply(stream(ZulipClient.stream), topic)(content)
    funit // don't wait for zulip

  def apply[C: Show](stream: String, topic: String)(content: C): Funit =
    send(ZulipMessage(stream = stream, topic = topic, content = content.show))
    funit // don't wait for zulip

  def sendAndGetLink(stream: ZulipClient.stream.Selector, topic: String)(
      content: String
  ): Fu[Option[String]] = sendAndGetLink(stream(ZulipClient.stream), topic)(content)

  def sendAndGetLink(stream: String, topic: String)(
      content: String
  ): Fu[Option[String]] =
    send(ZulipMessage(stream = stream, topic = topic, content = content)).map:
      // Can be `None` if the message was rate-limited (i.e Someone already created a conv a few minutes earlier)
      _.map: msgId =>
        s"https://${config.domain}/#narrow/stream/${urlencode(stream)}/topic/${urlencode(topic)}/near/$msgId"

  private def send(msg: ZulipMessage): Fu[Option[ZulipMessage.ID]] = dedupMsg(msg).so:
    if config.domain.isEmpty then fuccess(lila.log("zulip").info(msg.toString)).inject(None)
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
        .flatMap:
          case res if res.status == 200 =>
            (res.body[JsValue] \ "id").validate[ZulipMessage.ID] match
              case JsSuccess(result, _) => fuccess(result.some)
              case JsError(err)         => fufail(s"[zulip]: $err, $msg ${res.status} ${res.body}")
          case res => fufail(s"[zulip] $msg ${res.status} ${res.body}")
        .monSuccess(_.irc.zulip.say(msg.stream))
        .logFailure(lila.log("zulip"))
        .recoverDefault

private object ZulipClient:

  case class Config(domain: String, user: String, pass: Secret)
  import lila.common.autoconfig.*
  import lila.common.config.given
  import lila.core.irc.ModDomain
  given ConfigLoader[Config] = AutoConfig.loader[Config]

  object stream:
    object mod:
      val log          = "mod-log"
      val adminLog     = "mod-admin-log"
      val adminGeneral = "mod-admin-general"
      val commsPublic  = "mod-comms-public"
      val commsPrivate = "mod-comms-private"
      val hunterCheat  = "mod-hunter-cheat"
      val hunterBoost  = "mod-hunter-boost"
      val adminAppeal  = "mod-admin-appeal"
      val cafeteria    = "mod-cafeteria"
      val usernames    = "mod-usernames"
      val trustSafety  = "org-trustsafety"
      def adminMonitor(tpe: ModDomain) = tpe match
        case ModDomain.Comm  => "mod-admin-monitor-comm"
        case ModDomain.Cheat => "mod-admin-monitor-cheat"
        case ModDomain.Boost => "mod-admin-monitor-boost"
        case _               => "mod-admin-monitor-other"
    val general   = "general"
    val broadcast = "content-broadcast"
    val blog      = "content-blog"
    val content   = "content-site"
    type Selector = ZulipClient.stream.type => String

private case class ZulipMessage(
    stream: String,
    topic: String,
    content: String
):

  override def toString = s"[$stream:$topic] $content"

private object ZulipMessage:
  type ID = Int
