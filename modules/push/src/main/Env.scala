package lila.push

import akka.actor._
import com.google.auth.oauth2.{ GoogleCredentials, ServiceAccountCredentials }
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient
import scala.jdk.CollectionConverters._

import lila.common.config._
import FirebasePush.configLoader

@Module
final private class PushConfig(
    @ConfigName("collection.device") val deviceColl: CollName,
    @ConfigName("collection.subscription") val subscriptionColl: CollName,
    val web: WebPush.Config,
    val firebase: FirebasePush.Config
)

final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    db: lila.db.Db,
    userRepo: lila.user.UserRepo,
    getLightUser: lila.common.LightUser.Getter,
    proxyRepo: lila.round.GameProxyRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[PushConfig]("push")(AutoConfig.loader)

  def vapidPublicKey = config.web.vapidPublicKey

  private lazy val deviceApi  = new DeviceApi(db(config.deviceColl))
  lazy val webSubscriptionApi = new WebSubscriptionApi(db(config.subscriptionColl))

  def registerDevice    = deviceApi.register _
  def unregisterDevices = deviceApi.unregister _

  private lazy val googleCredentials: Option[GoogleCredentials] =
    try {
      config.firebase.json.value.some.filter(_.nonEmpty).map { json =>
        ServiceAccountCredentials
          .fromStream(new java.io.ByteArrayInputStream(json.getBytes()))
          .createScoped(Set("https://www.googleapis.com/auth/firebase.messaging").asJava)
      }
    } catch {
      case e: Exception =>
        logger.warn("Failed to create google credentials", e)
        none
    }
  if (googleCredentials.isDefined) logger.info("Firebase push notifications are enabled.")

  private lazy val firebasePush = wire[FirebasePush]

  private lazy val webPush = wire[WebPush]

  private lazy val pushApi: PushApi = wire[PushApi]

  private def logUnit(f: Fu[_]): Unit = {
    f logFailure logger
    ()
  }
  lila.common.Bus.subscribeFun(
    "finishGame",
    "moveEventCorres",
    "newMessage",
    "msgUnread",
    "challenge",
    "corresAlarm",
    "offerEventCorres"
  ) {
    case lila.game.actorApi.FinishGame(game, _, _) =>
      logUnit { pushApi finish game }
    case lila.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable =>
      logUnit { pushApi move move }
    case lila.hub.actorApi.round.CorresTakebackOfferEvent(gameId) =>
      logUnit { pushApi takebackOffer gameId }
    case lila.hub.actorApi.round.CorresDrawOfferEvent(gameId) =>
      logUnit { pushApi drawOffer gameId }
    case lila.msg.MsgThread.Unread(t) =>
      logUnit { pushApi newMsg t }
    case lila.challenge.Event.Create(c) =>
      logUnit { pushApi challengeCreate c }
    case lila.challenge.Event.Accept(c, joinerId) =>
      logUnit { pushApi.challengeAccept(c, joinerId) }
    case lila.game.actorApi.CorresAlarmEvent(pov) =>
      logUnit { pushApi corresAlarm pov }
  }
}
