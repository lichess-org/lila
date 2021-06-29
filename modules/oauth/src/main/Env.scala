package lila.oauth

import akka.actor._
import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._
import lila.db.AsyncColl

private case class OauthConfig(
    @ConfigName("mongodb.uri") mongoUri: String,
    @ConfigName("collection.access_token") tokenColl: CollName,
    @ConfigName("collection.app") appColl: CollName
)

@Module
final class Env(
    appConfig: Configuration,
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    lilaDb: lila.db.Db,
    mongo: lila.db.Env
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val config = appConfig.get[OauthConfig]("oauth")(AutoConfig.loader)

  private lazy val db = mongo.asyncDb("oauth", config.mongoUri)

  private lazy val colls = new OauthColls(db(config.tokenColl), db(config.appColl))

  lazy val appApi = wire[OAuthAppApi]

  lazy val server = wire[OAuthServer]

  lazy val tryServer: OAuthServer.Try = () =>
    scala.concurrent
      .Future {
        server.some
      }
      .withTimeoutDefault(50 millis, none) recover { case e: Exception =>
      lila.log("security").warn("oauth", e)
      none
    }

  lazy val tokenApi         = wire[AccessTokenApi]
  lazy val authorizationApi = new AuthorizationApi(lilaDb(CollName("oauth2_authorization")))
  lazy val legacyClientApi  = new LegacyClientApi(lilaDb(CollName("oauth2_legacy_client")))

  def forms = OAuthForm
}

private class OauthColls(val token: AsyncColl, val app: AsyncColl)
