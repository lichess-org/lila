package lila.oauth

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration

import lila.common.config.CollName
import lila.common.Strings
import lila.memo.SettingStore.Strings.given
import lila.common.config.Secret

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    userRepo: lila.user.UserRepo,
    settingStore: lila.memo.SettingStore.Builder,
    appConfig: Configuration,
    db: lila.db.Db
)(using Executor):

  lazy val originBlocklistSetting = settingStore[Strings](
    "oauthOriginBlocklist",
    default = Strings(Nil),
    text = "OAuth origin blocklist".some
  ).taggedWith[OriginBlocklist]

  lazy val legacyClientApi = LegacyClientApi(db(CollName("oauth2_legacy_client")))

  lazy val authorizationApi = AuthorizationApi(db(CollName("oauth2_authorization")))

  lazy val tokenApi = AccessTokenApi(db(CollName("oauth2_access_token")), cacheApi, userRepo)

  private val mobileSecret = appConfig.get[Secret]("oauth.mobile.secret").taggedWith[MobileSecret]
  lazy val server          = wire[OAuthServer]

trait OriginBlocklist
trait MobileSecret
