package lila.oauth

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config.CollName

@Module
final class Env(
    appConfig: Configuration,
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    db: lila.db.Db,
    mongo: lila.db.Env
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  lazy val legacyClientApi  = new LegacyClientApi(db(CollName("oauth2_legacy_client")))
  lazy val authorizationApi = new AuthorizationApi(db(CollName("oauth2_authorization")))
  lazy val tokenApi         = new AccessTokenApi(db(CollName("oauth2_access_token")), cacheApi, userRepo)
  lazy val server           = wire[OAuthServer]
}
