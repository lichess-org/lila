package lila.oidc

import java.net.URI

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.openid.connect.sdk.SubjectType
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.nimbusds.oauth2.sdk.id.Issuer

class JwsAlgorithmValidationTest extends munit.FunSuite:

  test("accepts configured asymmetric algorithms") {
    assertEquals(JwsAlgorithmValidation.parse("RS256"), Right(JWSAlgorithm.RS256))
    assertEquals(JwsAlgorithmValidation.parse(" ES256 "), Right(JWSAlgorithm.ES256))
  }

  test("rejects empty, none, HMAC and unknown algorithms") {
    assert(JwsAlgorithmValidation.parse("").isLeft)
    assert(JwsAlgorithmValidation.parse("none").isLeft)
    assert(JwsAlgorithmValidation.parse("HS256").isLeft)
    assert(JwsAlgorithmValidation.parse("bogus").isLeft)
  }

  test("factory fails at startup when jws_alg is empty") {
    intercept[IllegalArgumentException] {
      OIDCFactory(
        "https://issuer.example",
        "client",
        "secret",
        "https://rp.example/signin-oidc",
        "",
        "openid profile email",
        "client_secret_post"
      )
    }
  }

  test("factory requires openid scope") {
    intercept[IllegalArgumentException] {
      OIDCFactory(
        "https://issuer.example",
        "client",
        "secret",
        "https://rp.example/signin-oidc",
        "RS256",
        "profile email",
        "client_secret_post"
      )
    }
  }

  test("factory rejects unknown token auth method") {
    intercept[IllegalArgumentException] {
      OIDCFactory(
        "https://issuer.example",
        "client",
        "secret",
        "https://rp.example/signin-oidc",
        "RS256",
        "openid profile email",
        "private_key_jwt"
      )
    }
  }

  test("algorithm is accepted only when advertised by provider metadata") {
    val metadata = new OIDCProviderMetadata(
      new Issuer("https://issuer.example"),
      java.util.List.of(SubjectType.PUBLIC),
      new URI("https://issuer.example/jwks")
    )
    metadata.setIDTokenJWSAlgs(java.util.List.of(JWSAlgorithm.ES256))
    assertEquals(
      JwsAlgorithmValidation.ensureSupported(JWSAlgorithm.ES256)(metadata),
      Right(JWSAlgorithm.ES256)
    )
    assert(JwsAlgorithmValidation.ensureSupported(JWSAlgorithm.RS256)(metadata).isLeft)
  }
