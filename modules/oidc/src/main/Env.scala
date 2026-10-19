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
    @ConfigName("issuer.jws_alg") val jwsAlgorithm: String
)

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    userRepo: lila.user.UserRepo,
    authenticator: lila.core.security.Authenticator
)(using Executor, akka.stream.Materializer):
  import net.baseUrl

  private val config = appConfig.get[OidcConfig]("oidc")(AutoConfig.loader)

  private lazy val oidc = OIDCFactory.apply(
    config.issuer,
    config.clientId,
    config.clientSecret.value,
    s"$baseUrl/signin-oidc",
    config.jwsAlgorithm
  )

  lazy val api = wire[OidcApi]
