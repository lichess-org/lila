package lila.relation

import lila.db.Implicits._
import lila.db.api._
import tube.relationTube

import play.api.libs.json._

private[relation] object RelationRepo {

  def relation(id: ID): Fu[Option[Relation]] =
    $primitive.one($select byId id, "r")(_.asOpt[Boolean])

  def relation(u1: ID, u2: ID): Fu[Option[Relation]] = relation(makeId(u1, u2))

  def followers(userId: ID) = relaters(userId, Follow)
  def following(userId: ID) = relating(userId, Follow)

  def blockers(userId: ID) = relaters(userId, Block)
  def blocking(userId: ID) = relating(userId, Block)

  private def relaters(userId: ID, relation: Boolean): Fu[Set[ID]] =
    $primitive(Json.obj("u2" -> userId, "r" -> relation), "u1")(_.asOpt[ID]) map (_.toSet)

  private def relating(userId: ID, relation: Boolean): Fu[Set[ID]] =
    $primitive(Json.obj("u1" -> userId, "r" -> relation), "u2")(_.asOpt[ID]) map (_.toSet)

  def follow(u1: ID, u2: ID): Funit = save(u1, u2, Follow)
  def unfollow(u1: ID, u2: ID): Funit = remove(u1, u2)
  def block(u1: ID, u2: ID): Funit = save(u1, u2, Block)
  def unblock(u1: ID, u2: ID): Funit = remove(u1, u2)

  private def save(u1: ID, u2: ID, relation: Relation): Funit = $save(
    makeId(u1, u2),
    Json.obj("u1" -> u1, "u2" -> u2, "r" -> relation)
  )

  def remove(u1: ID, u2: ID): Funit = $remove byId makeId(u1, u2)

  private def makeId(u1: String, u2: String) = u1 + "/" + u2
}
