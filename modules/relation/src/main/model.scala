package lila.relation

def makeId(u1: UserId, u2: UserId) = s"$u1/$u2"

case class Follower(u1: UserId):
  def userId = u1

case class Followed(u2: UserId):
  def userId = u2

case class Blocked(u2: UserId):
  def userId = u2

case class Related[U](
    user: U,
    nbGames: Option[Int],
    followable: Boolean,
    relation: Option[Relation]
)

private object BSONHandlers:

  import reactivemongo.api.bson.*
  import lila.db.dsl.given

  given BSONDocumentHandler[Follower] = Macros.handler
  given BSONDocumentHandler[Followed] = Macros.handler
  given BSONDocumentHandler[Blocked] = Macros.handler
