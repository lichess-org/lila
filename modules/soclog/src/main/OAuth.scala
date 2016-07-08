package lila.soclog

import org.joda.time.DateTime

case class OAuth(
    _id: String, // provider:extId
    provider: String,
    extId: String, // user ID on the social service
    tokens: AccessToken,
    createdAt: DateTime,
    updatedAt: DateTime) {

  def id = _id
}

object OAuth {

  def makeId(provider: Provider, profile: Profile) =
    s"${provider.name}:${profile.userId}"

  def make(provider: Provider, profile: Profile, tokens: AccessToken) = OAuth(
    _id = makeId(provider, profile),
    provider = provider.name,
    extId = profile.userId,
    tokens = tokens,
    createdAt = DateTime.now,
    updatedAt = DateTime.now)

  object BSONHandlers {
    import lila.db.BSON
    import lila.db.dsl._
    import reactivemongo.bson._
    implicit val AccessTokenHandler = Macros.handler[AccessToken]
    implicit val OAuthHandler = Macros.handler[OAuth]
  }
}
