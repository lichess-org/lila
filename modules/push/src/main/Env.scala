package lila.push

import akka.actor._
import com.typesafe.config.Config
import java.io.InputStream

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    getLightUser: String => Option[lila.common.LightUser],
    roundSocketHub: ActorSelection,
    system: ActorSystem) {

  private val CollectionDevice = config getString "collection.device"
  private val GooglePushUrl = config getString "google.url"
  private val GooglePushKey = config getString "google.key"
  private val OneSignalUrl = config getString "onesignal.url"
  private val OneSignalAppId = config getString "onesignal.app_id"
  private val OneSignalKey = config getString "onesignal.key"

  private lazy val deviceApi = new DeviceApi(db(CollectionDevice))

  def registerDevice = deviceApi.register _
  def unregisterDevices = deviceApi.unregister _

  private lazy val oneSignalPush = new OneSignalPush(
    deviceApi.findLastByUserId("onesignal") _,
    url = OneSignalUrl,
    appId = OneSignalAppId,
    key = OneSignalKey)

  private lazy val googlePush = new GooglePush(
    deviceApi.findLastByUserId("android") _,
    url = GooglePushUrl,
    key = GooglePushKey)

  private lazy val pushApi = new PushApi(
    googlePush,
    oneSignalPush,
    getLightUser,
    roundSocketHub)

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    import akka.pattern.pipe
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => pushApi finish game
      case move: lila.hub.actorApi.round.MoveEvent   => pushApi move move
      case lila.challenge.Event.Create(c)            => pushApi challengeCreate c
      case lila.challenge.Event.Accept(c, joinerId)  => pushApi.challengeAccept(c, joinerId)
    }
  })), 'finishGame, 'moveEvent, 'challenge)
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    roundSocketHub = lila.hub.Env.current.socket.round,
    config = lila.common.PlayApp loadConfig "push")
}
