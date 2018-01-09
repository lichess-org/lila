package lila.push

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    getLightUser: lila.common.LightUser.GetterSync,
    roundSocketHub: ActorSelection,
    scheduler: lila.common.Scheduler,
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

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => pushApi finish game
      case lila.hub.actorApi.round.CorresMoveEvent(move, _, pushable, _, _) if pushable => pushApi move move
      case lila.hub.actorApi.round.CorresTakebackOfferEvent(gameId) => pushApi takebackOffer gameId
      case lila.hub.actorApi.round.CorresDrawOfferEvent(gameId) => pushApi drawOffer gameId
      case lila.message.Event.NewMessage(t, p) => pushApi newMessage (t, p)
      case lila.challenge.Event.Create(c) => pushApi challengeCreate c
      case lila.challenge.Event.Accept(c, joinerId) => pushApi.challengeAccept(c, joinerId)
      case lila.game.actorApi.CorresAlarmEvent(pov) => pushApi corresAlarm pov
    }
  })), 'finishGame, 'moveEventCorres, 'newMessage, 'challenge, 'corresAlarm, 'offerEventCorres)
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUserSync,
    roundSocketHub = lila.hub.Env.current.socket.round,
    scheduler = lila.common.PlayApp.scheduler,
    config = lila.common.PlayApp loadConfig "push"
  )
}
