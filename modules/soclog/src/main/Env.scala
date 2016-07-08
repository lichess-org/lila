package lila.soclog

import akka.actor._
import com.typesafe.config.Config
import configs.syntax._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem) {

  private val BaseUrl = config getString "base_url"
  private val CollectionOAuth = config getString "collection.oauth"

  val providers: Providers =
    config.get[Providers]("providers") valueOrElse sys.error("soclog config")

  private val client = new OAuthClient(BaseUrl)

  val api = new SoclogApi(client, db(CollectionOAuth))
}

object Env {

  lazy val current: Env = "soclog" boot new Env(
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "soclog")
}
