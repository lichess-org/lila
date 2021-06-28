package lila.oauth

import org.joda.time.DateTime

import lila.db.dsl._

final class LegacyClientApi(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {
  import LegacyClientApi.{ BSONFields => F }

  def apply(clientId: Protocol.ClientId): Fu[Option[Protocol.HashedClientSecret]] =
    coll
      .findAndUpdate($id(clientId.value), $set(F.usedAt -> DateTime.now()))
      .map {
        _.result[Bdoc].flatMap(_.getAsOpt[String](F.hashedSecret)).map(Protocol.HashedClientSecret)
      }
}

object LegacyClientApi {
  object BSONFields {
    val id           = "_id"
    val hashedSecret = "secret"
    val usedAt       = "used"
  }
}
