package lila.soclog

import akka.actor._
import com.typesafe.config.Config
import configs.syntax._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    system: ActorSystem) {

  private val BaseUrl = config getString "net.base_url"

  val providers: Providers =
    config.get[Providers]("providers") valueOrElse sys.error("soclog config")

  private val client = new OAuthClient(BaseUrl)

  val api = new SoclogApi(client)
}

object Env {

  lazy val current: Env = "soclog" boot new Env(
    system = lila.common.PlayApp.system,
    config = lila.common.PlayApp loadConfig "soclog")
}
