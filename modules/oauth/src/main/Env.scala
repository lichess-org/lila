package lila.oauth

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*

import lila.common.config.CollName
import lila.common.Strings
import lila.memo.SettingStore.Strings.given

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    settingStore: lila.memo.SettingStore.Builder,
    db: lila.db.Db
)(using ec: scala.concurrent.ExecutionContext):

  lazy val originBlocklistSetting = settingStore[Strings](
    "oauthOriginBlocklist",
    default = Strings(Nil),
    text = "OAuth origin blocklist".some
  ).taggedWith[OriginBlocklist]

  lazy val legacyClientApi = new LegacyClientApi(db(CollName("oauth2_legacy_client")))

  lazy val authorizationApi = new AuthorizationApi(db(CollName("oauth2_authorization")))

  lazy val tokenApi = new AccessTokenApi(db(CollName("oauth2_access_token")), cacheApi, userRepo)

  lazy val server = wire[OAuthServer]

trait OriginBlocklist
