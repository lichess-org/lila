package lila.oidc

import java.time.Instant

import reactivemongo.api.bson.*

class OidcStateTest extends munit.FunSuite:

  import OidcLoginState.given

  test("login state round-trips with _id") {
    val at = Instant.parse("2026-10-20T06:00:00Z")
    val state = OidcLoginState(
      state = "state-1",
      nonce = "nonce-1",
      codeVerifier = "verifier-1",
      referrer = Some("/tournament"),
      createdAt = at,
      expiresAt = at.plusSeconds(600)
    )
    val doc = BSON.writeDocument(state).get
    assertEquals(doc.getAsOpt[String]("_id"), Some("state-1"))
    assertEquals(BSON.readDocument[OidcLoginState](doc).toOption, Some(state))
  }
