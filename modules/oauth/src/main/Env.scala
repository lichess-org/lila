package lila.oauth

import com.softwaremill.macwire._

import lila.common.config.CollName

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {
  lazy val legacyClientApi  = new LegacyClientApi(db(CollName("oauth2_legacy_client")))
  lazy val authorizationApi = new AuthorizationApi(db(CollName("oauth2_authorization")))
  lazy val tokenApi         = new AccessTokenApi(db(CollName("oauth2_access_token")), cacheApi, userRepo)
  lazy val server           = wire[OAuthServer]
}
