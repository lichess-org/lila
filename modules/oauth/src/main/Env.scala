package lila.oauth

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.db.DbConfig
import lila.db.dsl.Coll
import lila.db.Env.configLoader

private case class OauthConfig(
    mongodb: DbConfig,
    @ConfigName("collection.access_token") tokenColl: CollName,
    @ConfigName("collection.app") appColl: CollName
)

@Module
final class Env(
    appConfig: Configuration,
    asyncCache: lila.memo.AsyncCache.Builder,
    userRepo: lila.user.UserRepo,
    system: ActorSystem,
    lifecycle: play.api.inject.ApplicationLifecycle
) {

  private val config = appConfig.get[OauthConfig]("oauth")(AutoConfig.loader)

  private lazy val db = new lila.db.Env("oauth", config.mongodb)
  private lazy val tokenColl = db(config.tokenColl)
  private lazy val appColl = db(config.appColl)

  lazy val appApi = new OAuthAppApi(appColl)

  // #TODO lila should be able to start without it
  lazy val server = {
    val mk = (coll: Coll) => wire[OAuthServer]
    mk(tokenColl)
  }

  lazy val tryServer: OAuthServer.Try = () => scala.concurrent.Future {
    server.some
  }.withTimeoutDefault(50 millis, none)(system) recover {
    case e: Exception =>
      lila.log("security").warn("oauth", e)
      none
  }

  lazy val tokenApi = new PersonalTokenApi(
    tokenColl = tokenColl
  )

  def forms = OAuthForm
}
