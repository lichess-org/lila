package lila.push

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getLightUser: lila.common.LightUser.GetterSync,
    scheduler: lila.common.Scheduler,
    system: ActorSystem
) {

  private val CollectionDevice = config getString "collection.device"
  private val CollectionSubscription = config getString "collection.subscription"

  private val OneSignalUrl = config getString "onesignal.url"
  private val OneSignalAppId = config getString "onesignal.app_id"
  private val OneSignalKey = config getString "onesignal.key"

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

  private lazy val webPush = new WebPush(
    webSubscriptionApi.getSubscriptions(5) _,
    url = WebUrl,
    vapidPublicKey = WebVapidPublicKey
  )

  private lazy val pushApi = new PushApi(
    oneSignalPush,
    webPush,
    getLightUser,
    bus = system.lilaBus,
    scheduler = scheduler
  )

  system.lilaBus.subscribeFun('finishGame, 'moveEventCorres, 'newMessage, 'challenge, 'corresAlarm, 'offerEventCorres) {
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
    scheduler = lila.common.PlayApp.scheduler,
    config = lila.common.PlayApp loadConfig "push"
  )
}
