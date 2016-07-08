package lila.soclog
package oauth1

import org.joda.time.DateTime

case class OAuth1(
    _id: String, // random
    provider: String,
    tokens: OAuth1AccessToken,
    profile: Profile,
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id
}

object OAuth1 {

  def make(provider: OAuth1Provider, profile: Profile, tokens: OAuth1AccessToken) = OAuth1(
    _id = ornicar.scalalib.Random nextStringUppercase 10,
    provider = provider.name,
    profile = profile,
    tokens = tokens,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)

  object BSONHandlers {
    import lila.db.BSON
    import lila.db.dsl._
    import reactivemongo.bson._
    implicit val OAuth1AccessTokenHandler = Macros.handler[OAuth1AccessToken]
    implicit val ProfileHandler = Macros.handler[Profile]
    implicit val OAuth1Handler = Macros.handler[OAuth1]
  }
}
