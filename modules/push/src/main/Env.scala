package lidraughts.push

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lidraughts.db.Env,
    getLightUser: lidraughts.common.LightUser.GetterSync,
    roundSocketHub: ActorSelection,
    scheduler: lidraughts.common.Scheduler,
    system: ActorSystem
) {

  private val CollectionDevice = config getString "collection.device"
  private val OneSignalUrl = config getString "onesignal.url"
  private val OneSignalAppId = config getString "onesignal.app_id"
  private val OneSignalKey = config getString "onesignal.key"

  private lazy val deviceApi = new DeviceApi(db(CollectionDevice))

  def registerDevice = deviceApi.register _
  def unregisterDevices = deviceApi.unregister _

  private lazy val oneSignalPush = new OneSignalPush(
    deviceApi.findLastManyByUserId("onesignal", 3) _,
    url = OneSignalUrl,
    appId = OneSignalAppId,
    key = OneSignalKey
  )

  private lazy val pushApi = new PushApi(
    oneSignalPush,
    getLightUser,
    roundSocketHub,
    scheduler = scheduler
  )

  system.lidraughtsBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.game.actorApi.FinishGame(game, _, _) => pushApi finish game
      case lidraughts.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable => pushApi move move
      case lidraughts.hub.actorApi.round.CorresTakebackOfferEvent(gameId) => pushApi takebackOffer gameId
      case lidraughts.hub.actorApi.round.CorresDrawOfferEvent(gameId) => pushApi drawOffer gameId
      case lidraughts.message.Event.NewMessage(t, p) => pushApi newMessage (t, p)
      case lidraughts.challenge.Event.Create(c) => pushApi challengeCreate c
      case lidraughts.challenge.Event.Accept(c, joinerId) => pushApi.challengeAccept(c, joinerId)
      case lidraughts.game.actorApi.CorresAlarmEvent(pov) => pushApi corresAlarm pov
    }
  })), 'finishGame, 'moveEventCorres, 'newMessage, 'challenge, 'corresAlarm, 'offerEventCorres)
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lidraughts.db.Env.current,
    system = lidraughts.common.PlayApp.system,
    getLightUser = lidraughts.user.Env.current.lightUserSync,
    roundSocketHub = lidraughts.hub.Env.current.socket.round,
    scheduler = lidraughts.common.PlayApp.scheduler,
    config = lidraughts.common.PlayApp loadConfig "push"
  )
}
