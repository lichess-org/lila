package lila.relation

case class Follower(u1: String):
  def userId = u1

case class Followed(u2: String):
  def userId = u2

case class Blocked(u2: String):
  def userId = u2

case class Related(
    user: lila.user.User,
    nbGames: Option[Int],
    followable: Boolean,
    relation: Option[Relation]
)

case class Relations(
    in: Option[Relation],
    out: Option[Relation]
)

object BSONHandlers:

  import reactivemongo.api.bson.*

  private[relation] given BSONDocumentHandler[Follower] = Macros.handler
  private[relation] given BSONDocumentHandler[Followed] = Macros.handler
  private[relation] given BSONDocumentHandler[Blocked]  = Macros.handler
