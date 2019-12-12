package lila.push

import akka.actor._
import collection.JavaConverters._
import com.google.auth.oauth2.{ GoogleCredentials, ServiceAccountCredentials }
import com.typesafe.config.Config
import play.api.Play
import Play.current

import lila.game.Game

final class Env(
    config: Config,
    db: lila.db.Env,
    getLightUser: lila.common.LightUser.GetterSync,
    gameProxy: Game.ID => Fu[Option[Game]],
    scheduler: lila.common.Scheduler,
    system: ActorSystem
) {

  private val CollectionDevice = config getString "collection.device"
  private val CollectionSubscription = config getString "collection.subscription"

  private val OneSignalUrl = config getString "onesignal.url"
  private val OneSignalAppId = config getString "onesignal.app_id"
  private val OneSignalKey = config getString "onesignal.key"

  private val FirebaseUrl = config getString "firebase.url"

  private val WebUrl = config getString "web.url"
  val WebVapidPublicKey = config getString "web.vapid_public_key"

  private lazy val deviceApi = new DeviceApi(db(CollectionDevice))
  lazy val webSubscriptionApi = new WebSubscriptionApi(db(CollectionSubscription))

  def registerDevice = deviceApi.register _
  def unregisterDevices = deviceApi.unregister _

  private lazy val oneSignalPush = new OneSignalPush(
    deviceApi.findLastManyByUserId("onesignal", 3) _,
    url = OneSignalUrl,
    appId = OneSignalAppId,
    key = OneSignalKey
  )

  val googleCredentials: Option[GoogleCredentials] = try {
    config.getString("firebase.json").some.filter(_.nonEmpty).map { json =>
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

  private lazy val firebasePush = new FirebasePush(
    googleCredentials,
    deviceApi.findLastManyByUserId("firebase", 3) _,
    url = FirebaseUrl
  )(system)

  private lazy val webPush = new WebPush(
    webSubscriptionApi.getSubscriptions(5) _,
    url = WebUrl,
    vapidPublicKey = WebVapidPublicKey
  )

  private lazy val pushApi = new PushApi(
    firebasePush,
    oneSignalPush,
    webPush,
    getLightUser,
    gameProxy,
    scheduler = scheduler,
    system = system
  )

  lila.common.Bus.subscribeFun('finishGame, 'moveEventCorres, 'newMessage, 'challenge, 'corresAlarm, 'offerEventCorres) {
    case lila.game.actorApi.FinishGame(game, _, _) => pushApi finish game logFailure logger
    case lila.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable => pushApi move move logFailure logger
    case lila.hub.actorApi.round.CorresTakebackOfferEvent(gameId) => pushApi takebackOffer gameId logFailure logger
    case lila.hub.actorApi.round.CorresDrawOfferEvent(gameId) => pushApi drawOffer gameId logFailure logger
    case lila.message.Event.NewMessage(t, p) => pushApi newMessage (t, p) logFailure logger
    case lila.challenge.Event.Create(c) => pushApi challengeCreate c logFailure logger
    case lila.challenge.Event.Accept(c, joinerId) => pushApi.challengeAccept(c, joinerId) logFailure logger
    case lila.game.actorApi.CorresAlarmEvent(pov) => pushApi corresAlarm pov logFailure logger
  }
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUserSync,
    gameProxy = lila.round.Env.current.proxy.game _,
    scheduler = lila.common.PlayApp.scheduler,
    config = lila.common.PlayApp loadConfig "push"
  )
}
