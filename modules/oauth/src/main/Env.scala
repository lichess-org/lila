package lila.oauth

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val settings = new {
    val DbConfig = config getConfig "mongodb"
    val CollectionAccessToken = config getString "collection.access_token"
    val CollectionApp = config getString "collection.app"
    val JwtPublicKey = config getString "jwt.public_key"
  }
  import settings._

  val baseUrl = config getString "base_url"

  private val db = new lila.db.Env("oauth", DbConfig, lifecycle)
  private val tokenColl = db(CollectionAccessToken)
  private val appColl = db(CollectionApp)

  lazy val server = new OAuthServer(
    tokenColl = tokenColl,
    jwtPublicKey = JWT.PublicKey(JwtPublicKey)
  )

  lazy val tokenApi = new PersonalTokenApi(
    tokenColl = tokenColl
  )

  lazy val appApi = new OAuthAppApi(appColl)

  def forms = OAuthForm
}

object Env {

  lazy val current = "oauth" boot new Env(
    config = lila.common.PlayApp loadConfig "oauth",
    system = lila.common.PlayApp.system,
    lifecycle = lila.common.PlayApp.lifecycle
  )
}
