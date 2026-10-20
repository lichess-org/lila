package lila.oidc

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.common.config.{ *, given }
import lila.core.config.*

@Module
private case class OidcConfig(
    @ConfigName("issuer.base_url") val issuer: String,
    @ConfigName("issuer.client_id") val clientId: String,
    @ConfigName("issuer.client_secret") val clientSecret: Secret,
    @ConfigName("issuer.scopes") val scopes: String,
    @ConfigName("issuer.jws_alg") val jwsAlgorithm: String,
    @ConfigName("issuer.token_auth_method") val tokenAuthMethod: String
)

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    userRepo: lila.user.UserRepo,
    identity: lila.identity.Env,
    db: lila.db.Db,
    mode: play.api.Mode
)(using Executor, akka.stream.Materializer):
  import net.baseUrl

  private val config = appConfig.get[OidcConfig]("oidc")(AutoConfig.loader)

  if mode.isProd then
    if config.clientId.isEmpty then
      throw new IllegalArgumentException("oidc.issuer.client_id must be configured in production")
    if config.clientSecret.value.isEmpty then
      throw new IllegalArgumentException("oidc.issuer.client_secret must be configured in production")

  private lazy val oidc = OIDCFactory.apply(
    config.issuer,
    config.clientId,
    config.clientSecret.value,
    s"$baseUrl/signin-oidc",
    config.jwsAlgorithm,
    config.scopes,
    config.tokenAuthMethod
  )

  private lazy val provisioning = UserProvisioning(userRepo, identity.statusRepo, identity.aliasRepo)

  private lazy val stateRepo = OidcStateRepo(db(lila.core.config.CollName("oidc_state")))

  lazy val api = OidcApi(oidc, provisioning, stateRepo)
