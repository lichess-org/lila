package lila.oauth

import org.joda.time.DateTime
import com.roundeights.hasher.Algo

import lila.db.dsl._

final class LegacyClientApi(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  import LegacyClientApi.{ BSONFields => F, _ }

  def apply(clientId: Protocol.ClientId, redirectUri: Protocol.RedirectUri): Fu[Option[HashedClientSecret]] =
    coll
      .findAndUpdate(
        $doc(F.id     -> clientId.value, F.redirectUri -> redirectUri.value.toString),
        $set(F.usedAt -> DateTime.now())
      )
      .map {
        _.result[Bdoc].flatMap(_.getAsOpt[String](F.hashedSecret)).map(HashedClientSecret)
      }
}

object LegacyClientApi {
  object BSONFields {
    val id           = "_id"
    val redirectUri  = "redirectUri"
    val hashedSecret = "secret"
    val usedAt       = "used"
  }

  case class HashedClientSecret(value: String) extends AnyVal

  case class ClientSecret(secret: String) extends AnyVal {
    def matches(hash: HashedClientSecret) = Algo.sha256(secret).hex == hash.value
    override def toString                 = "ClientSecret(***)"
  }

  case object MismatchingClientSecret
      extends Protocol.Error.InvalidGrant(
        "fix mismatching client secret (or update to pkce and update endpoints)"
      )
  case object ClientSecretRequired
      extends Protocol.Error.InvalidRequest("client_secret required (or update to pkce and update endpoints)")
  case object ClientSecretIgnored
      extends Protocol.Error.InvalidRequest(
        "cannot finish legacy flow with pkce token request (client_secret ignored, update to pkce or check endpoint)"
      )
  case object CodeVerifierIgnored
      extends Protocol.Error.InvalidRequest(
        "cannot finish pkce flow with legacy token request (code_verifier ignored, update endpoint)"
      )
}
