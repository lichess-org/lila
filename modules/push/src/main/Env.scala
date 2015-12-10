package lila.push

import com.typesafe.config.Config
import akka.actor._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    getLightUser: String => Option[lila.common.LightUser],
    roundSocketHub: ActorSelection,
    system: ActorSystem) {

  private val AerogearConfig = Aerogear.Config(
    url = config getString "aerogear.url",
    variantId = config getString "aerogear.variant_id",
    secret = config getString "aerogear.secret",
    applicationId = config getString "aerogear.application_id",
    masterSecret = config getString "aerogear.master_secret")

  private lazy val aerogear = new Aerogear(AerogearConfig)

  private lazy val api = new PushApi(aerogear, getLightUser, roundSocketHub)

  system.actorOf(Props(new Actor {
    override def preStart() {
      system.lilaBus.subscribe(self, 'finishGame, 'moveEvent)
    }
    import akka.pattern.pipe
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => api finish game
      case move: lila.hub.actorApi.round.MoveEvent   => api move move
    }
  }))
}

object Env {

  lazy val current: Env = "push" boot new Env(
    system = lila.common.PlayApp.system,
    getLightUser = lila.user.Env.current.lightUser,
    roundSocketHub = lila.hub.Env.current.socket.round,
    config = lila.common.PlayApp loadConfig "push")
}
