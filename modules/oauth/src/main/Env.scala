package lila.oauth

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration

import lila.common.config.given
import lila.core.config.{ CollName, Secret }
import lila.core.data.Strings
import lila.memo.SettingStore.Strings.given

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi,
    settingStore: lila.memo.SettingStore.Builder,
    appConfig: Configuration,
    db: lila.db.Db
)(using Executor, akka.stream.Materializer):

  lazy val originBlocklistSetting = settingStore[Strings](
    "oauthOriginBlocklist",
    default = Strings(Nil),
    text = "OAuth origin blocklist".some
  ).taggedWith[OriginBlocklist]

  lazy val legacyClientApi = LegacyClientApi(db(CollName("oauth2_legacy_client")))

  lazy val authorizationApi = AuthorizationApi(db(CollName("oauth2_authorization")))

  lazy val tokenApi = AccessTokenApi(db(CollName("oauth2_access_token")), cacheApi, userApi)

  private val mobileSecret = appConfig.get[Secret]("oauth.mobile.secret").taggedWith[MobileSecret]
  lazy val server          = wire[OAuthServer]

trait OriginBlocklist
trait MobileSecret
