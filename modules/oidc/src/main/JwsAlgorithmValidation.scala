package lila.oidc

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata

import scala.jdk.CollectionConverters.*

object JwsAlgorithmValidation:

  private val allowed: Set[JWSAlgorithm] = Set(
    JWSAlgorithm.RS256,
    JWSAlgorithm.RS384,
    JWSAlgorithm.RS512,
    JWSAlgorithm.ES256,
    JWSAlgorithm.ES384,
    JWSAlgorithm.ES512,
    JWSAlgorithm.PS256,
    JWSAlgorithm.PS384,
    JWSAlgorithm.PS512,
    JWSAlgorithm.EdDSA
  )

  /** Parse the configured `jws_alg`. Only asymmetric algorithms are accepted: `none` and HMAC (HS*)
    * signatures are forbidden for ID tokens.
    */
  def parse(raw: String): Either[String, JWSAlgorithm] =
    val value = raw.trim
    if value.isEmpty then Left("oidc.issuer.jws_alg must not be empty")
    else
      val alg = JWSAlgorithm.parse(value)
      if allowed.contains(alg) then Right(alg)
      else Left(s"oidc.issuer.jws_alg '$value' is not an allowed asymmetric signing algorithm")

  /** The configured algorithm must also be advertised by the provider metadata. */
  def ensureSupported(
      configured: JWSAlgorithm
  )(metadata: OIDCProviderMetadata): Either[String, JWSAlgorithm] =
    val supported = Option(metadata.getIDTokenJWSAlgs)
      .fold(Set.empty[JWSAlgorithm])(_.asScala.toSet)
    if supported.contains(configured) then Right(configured)
    else
      Left(
        s"oidc.issuer.jws_alg '${configured.getName}' is not advertised by the provider in id_token_signing_alg_values_supported"
      )
