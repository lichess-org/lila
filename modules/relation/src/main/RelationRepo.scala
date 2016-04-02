package lila.relation

import reactivemongo.bson._

import lila.db.dsl._

private[relation] object RelationRepo {

  // dirty
  private val coll = Env.current.coll

  def followers(userId: ID) = relaters(userId, Follow)
  def following(userId: ID) = relating(userId, Follow)

  def blockers(userId: ID) = relaters(userId, Block)
  def blocking(userId: ID) = relating(userId, Block)

  private def relaters(userId: ID, relation: Relation): Fu[Set[ID]] =
    coll.distinct("u1", BSONDocument(
      "u2" -> userId,
      "r" -> relation
    ).some) map lila.db.BSON.asStringSet

  private def relating(userId: ID, relation: Relation): Fu[Set[ID]] =
    coll.distinct("u2", BSONDocument(
      "u1" -> userId,
      "r" -> relation
    ).some) map lila.db.BSON.asStringSet

  def follow(u1: ID, u2: ID): Funit = save(u1, u2, Follow)
  def unfollow(u1: ID, u2: ID): Funit = remove(u1, u2)
  def block(u1: ID, u2: ID): Funit = save(u1, u2, Block)
  def unblock(u1: ID, u2: ID): Funit = remove(u1, u2)

  def unfollowAll(u1: ID): Funit = coll.remove($doc("u1" -> u1)).void

  private def save(u1: ID, u2: ID, relation: Relation): Funit = coll.update(
    $id(makeId(u1, u2)),
    $doc("u1" -> u1, "u2" -> u2, "r" -> relation),
    upsert = true).void

  def remove(u1: ID, u2: ID): Funit = coll.remove($id(makeId(u1, u2))).void

  def drop(userId: ID, relation: Relation, nb: Int) =
    coll.find(
      $doc("u1" -> userId, "r" -> relation),
      $doc("_id" -> true)
    ).sort($sort.naturalAsc)
      .hint($doc("u1" -> 1))
      .cursor[Bdoc]()
      .gather[List](nb).map {
        _.flatMap { _.getAs[String]("_id") }
      } flatMap { ids =>
        coll.remove($inIds(ids)).void
      }

  def makeId(u1: String, u2: String) = s"$u1/$u2"
}
