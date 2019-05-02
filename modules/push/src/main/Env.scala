package lidraughts.push

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    getLightUser: lidraughts.common.LightUser.GetterSync,
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
  private lazy val webSubscriptionApi = new WebSubscriptionApi(db(CollectionSubscription))

  def registerDevice = deviceApi.register _
  def unregisterDevices = deviceApi.unregister _
  def webSubscribe = webSubscriptionApi.subscribe _
  def webUnsubscribe = webSubscriptionApi.unsubscribe _

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
    bus = system.lidraughtsBus,
    scheduler = scheduler
  )

  system.lidraughtsBus.subscribeFun('finishGame, 'moveEventCorres, 'newMessage, 'challenge, 'corresAlarm, 'offerEventCorres) {
    case lidraughts.game.actorApi.FinishGame(game, _, _) => pushApi finish game
    case lidraughts.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable => pushApi move move
    case lidraughts.hub.actorApi.round.CorresTakebackOfferEvent(gameId) => pushApi takebackOffer gameId
    case lidraughts.hub.actorApi.round.CorresDrawOfferEvent(gameId) => pushApi drawOffer gameId
    case lidraughts.message.Event.NewMessage(t, p) => pushApi newMessage (t, p)
    case lidraughts.challenge.Event.Create(c) => pushApi challengeCreate c
    case lidraughts.challenge.Event.Accept(c, joinerId) => pushApi.challengeAccept(c, joinerId)
    case lidraughts.game.actorApi.CorresAlarmEvent(pov) => pushApi corresAlarm pov
  }
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lidraughts.db.Env.current,
    system = lidraughts.common.PlayApp.system,
    getLightUser = lidraughts.user.Env.current.lightUserSync,
    scheduler = lidraughts.common.PlayApp.scheduler,
    config = lidraughts.common.PlayApp loadConfig "push"
  )
}
