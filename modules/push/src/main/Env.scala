package lidraughts.push

import akka.actor._
import com.typesafe.config.Config

import lidraughts.game.Game

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    getLightUser: lidraughts.common.LightUser.GetterSync,
    gameProxy: Game.ID => Fu[Option[Game]],
    scheduler: lidraughts.common.Scheduler,
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
    gameProxy,
    bus = system.lidraughtsBus,
    scheduler = scheduler
  )

  system.lidraughtsBus.subscribeFun('finishGame, 'moveEventCorres, 'newMessage, 'challenge, 'corresAlarm, 'offerEventCorres) {
    case lidraughts.game.actorApi.FinishGame(game, _, _) => pushApi finish game logFailure logger
    case lidraughts.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable => pushApi move move logFailure logger
    case lidraughts.hub.actorApi.round.CorresTakebackOfferEvent(gameId) => pushApi takebackOffer gameId logFailure logger
    case lidraughts.hub.actorApi.round.CorresDrawOfferEvent(gameId) => pushApi drawOffer gameId logFailure logger
    case lidraughts.message.Event.NewMessage(t, p) => pushApi newMessage (t, p) logFailure logger
    case lidraughts.challenge.Event.Create(c) => pushApi challengeCreate c logFailure logger
    case lidraughts.challenge.Event.Accept(c, joinerId) => pushApi.challengeAccept(c, joinerId) logFailure logger
    case lidraughts.game.actorApi.CorresAlarmEvent(pov) => pushApi corresAlarm pov logFailure logger
  }
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lidraughts.db.Env.current,
    system = lidraughts.common.PlayApp.system,
    getLightUser = lidraughts.user.Env.current.lightUserSync,
    gameProxy = lidraughts.round.Env.current.proxy.game _,
    scheduler = lidraughts.common.PlayApp.scheduler,
    config = lidraughts.common.PlayApp loadConfig "push"
  )
}
