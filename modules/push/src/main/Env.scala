package lila.push

import akka.actor._
import com.typesafe.config.Config
import java.io.File

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    getLightUser: String => Option[lila.common.LightUser],
    isOnline: lila.user.User.ID => Boolean,
    roundSocketHub: ActorSelection,
    appleCertificate: File,
    system: ActorSystem) {

  private val CollectionDevice = config getString "collection.device"
  private val GooglePushUrl = config getString "google.url"
  private val GooglePushKey = config getString "google.key"
  private val ApplePushPassword = config getString "apple.password"

  private lazy val deviceApi = new DeviceApi(db(CollectionDevice))

  def registerDevice = deviceApi.register _
  def unregisterDevices = deviceApi.unregister _

  private lazy val googlePush = new GooglePush(
    deviceApi.findLastByUserId _,
    url = GooglePushUrl,
    key = GooglePushKey)

  private lazy val applePush = new ApplePush(
    deviceApi.findLastByUserId _,
    system = system,
    certificate = appleCertificate,
    password = ApplePushPassword)

  private lazy val pushApi = new PushApi(
    googlePush,
    applePush,
    getLightUser,
    isOnline,
    roundSocketHub)

  system.actorOf(Props(new Actor {
    override def preStart() {
      system.lilaBus.subscribe(self, 'finishGame, 'moveEvent, 'challenge)
    }
    import akka.pattern.pipe
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => pushApi finish game
      case move: lila.hub.actorApi.round.MoveEvent   => pushApi move move
      case lila.challenge.Event.Create(c)            => pushApi challengeCreate c
      case lila.challenge.Event.Accept(c, joinerId)  => pushApi.challengeAccept(c, joinerId)
    }
  }))
}

object Env {

  lazy val current: Env = "push" boot new Env(
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    isOnline = lila.user.Env.current.isOnline,
    roundSocketHub = lila.hub.Env.current.socket.round,
    appleCertificate = lila.common.PlayApp.withApp { app =>
      new File(app.classloader.getResource("zpns.p12").toURI)
    },
    config = lila.common.PlayApp loadConfig "push")
}
