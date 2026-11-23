package lila.oauth

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.Configuration
import scalalib.net.UserAgent

import lila.core.config.{ BaseUrl, CollName }
import lila.core.data.Strings
import lila.memo.SettingStore.Strings.given

@Module
final class Env(
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi,
    settingStore: lila.memo.SettingStore.Builder,
    appConfig: Configuration,
    baseUrl: BaseUrl,
    db: lila.db.Db
)(using Executor, org.apache.pekko.stream.Materializer, play.api.Mode):

  lazy val originBlocklistSetting = settingStore[Strings](
    "oauthOriginBlocklist",
    default = Strings(Nil),
    text = "OAuth origin blocklist".some
  ).taggedWith[OriginBlocklist]

  lazy val signedClients = wire[OAuthSignedClients]

  lazy val legacyClientApi = LegacyClientApi(db(CollName("oauth2_legacy_client")))

  lazy val authorizationApi = AuthorizationApi(db(CollName("oauth2_authorization")))

  lazy val tokenApi = AccessTokenApi(db(CollName("oauth2_access_token")), cacheApi, userApi)

  lazy val server = wire[OAuthServer]

  def makeToken(raw: AccessTokenRequest.Raw, legacy: Boolean)(using req: Req)(using UserAgent) =
    import cats.mtl.Handle.*
    allow:
      for
        prepared <-
          if legacy
          then raw.prepareLegacy(AccessTokenRequest.BasicAuth.from(req)).raiseIfLeft
          else raw.prepare.raiseIfLeft
        granted <- authorizationApi.consume(prepared)
        token <- tokenApi.create(granted)
      yield Right(AccessToken.json.token(token, legacy = true))
    .rescue: err =>
      fuccess(Left(err.toJson))

trait OriginBlocklist
