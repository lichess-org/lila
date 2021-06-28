package lila.oauth

import org.joda.time.DateTime
import com.roundeights.hasher.Algo

import lila.db.dsl._

final class LegacyClientApi(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  import LegacyClientApi.{ BSONFields => F, _ }

  def apply(clientId: Protocol.ClientId): Fu[Option[HashedClientSecret]] =
    coll
      .findAndUpdate($id(clientId.value), $set(F.usedAt -> DateTime.now()))
      .map {
        _.result[Bdoc].flatMap(_.getAsOpt[String](F.hashedSecret)).map(HashedClientSecret)
      }
}

object LegacyClientApi {
  object BSONFields {
    val id           = "_id"
    val hashedSecret = "secret"
    val usedAt       = "used"
  }

  case class HashedClientSecret(value: String) extends AnyVal

  case class ClientSecret(secret: String) extends AnyVal {
    def matches(hash: HashedClientSecret) = Algo.sha256(secret).hex == hash.value
    override def toString                 = "ClientSecret(***)"
  }

  case object MismatchingClientSecret
      extends Protocol.Error.InvalidGrant("fix mismatching client secret (or update to pkce)")
  case object ClientSecretRequired
      extends Protocol.Error.InvalidRequest("client_secret required (or update to pkce)")
}
