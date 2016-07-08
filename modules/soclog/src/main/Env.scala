package lila.soclog

import akka.actor._
import com.typesafe.config.Config
import configs.syntax._
import oauth1._
import oauth2._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: ActorSystem) {

  private val OAuth1CallbackUrl = config getString "oauth1.callback_url"
  private val OAuth1Collection = config getString "oauth1.collection"

  private val OAuth2Collection = config getString "oauth2.collection"

  object oauth1 {

    val providers: OAuth1Providers =
      config.get[OAuth1Providers]("oauth1.providers") valueOrElse sys.error("soclog config")

    private val client =
      new OAuth1Client(provider => OAuth1CallbackUrl.replace("<provider>", provider.name))

    val api = new OAuth1Api(client, db(OAuth1Collection))
  }

  object oauth2 {

    val providers: OAuth2Providers =
      config.get[OAuth2Providers]("oauth2.providers") valueOrElse sys.error("soclog config")
  }
}

object Env {

  lazy val current: Env = "soclog" boot new Env(
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current,
    config = lila.common.PlayApp loadConfig "soclog")
}
