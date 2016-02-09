package lila.explorer

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    system: ActorSystem) {

  private val Endpoint = config getString "endpoint"

  private lazy val indexer = new ExplorerIndexer(endpoint = Endpoint)

  def cli = new lila.common.Cli {
    def process = {
      case "explorer" :: "index" :: variant :: since :: Nil => indexer(variant, since) inject "done"
    }
  }

  def fetchPgn(id: String): Fu[Option[String]] = {
    import play.api.libs.ws.WS
    import play.api.Play.current
    WS.url(s"$Endpoint/master/pgn/$id").get() map {
      case res if res.status == 200 => res.body.some
      case _                        => None
    }
  }

  system.actorOf(Props(new Actor {
    context.system.lilaBus.subscribe(self, 'finishGame)
    def receive = {
      case lila.game.actorApi.FinishGame(game, _, _) => indexer(game)
    }
  }))
}

object Env {

  lazy val current = "explorer" boot new Env(
    config = lila.common.PlayApp loadConfig "explorer",
    system = lila.common.PlayApp.system)
}
