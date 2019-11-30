package lila.relation

import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.db.dsl._

private final class RelationRepo(coll: Coll) {

  import RelationRepo._

  def following(userId: ID) = relating(userId, Follow)

  def blockers(userId: ID) = relaters(userId, Block)
  def blocking(userId: ID) = relating(userId, Block)

  def followersFromSecondary(userId: ID) = relaters(userId, Follow, ReadPreference.secondaryPreferred)

  def followingLike(userId: ID, term: String): Fu[List[ID]] =
    lila.user.User.couldBeUsername(term) ?? {
      coll.secondaryPreferred.distinctEasy[ID, List]("u2", $doc(
        "u1" -> userId,
        "u2" $startsWith term.toLowerCase,
        "r" -> Follow
      ))
    }

  private def relaters(userId: ID, relation: Relation, rp: ReadPreference = ReadPreference.primary): Fu[Set[ID]] =
    coll.withReadPreference(rp).distinctEasy[String, Set]("u1", $doc(
      "u2" -> userId,
      "r" -> relation
    ))

  private def relating(userId: ID, relation: Relation): Fu[Set[ID]] =
    coll.distinctEasy[String, Set]("u2", $doc(
      "u1" -> userId,
      "r" -> relation
    ))

  def follow(u1: ID, u2: ID): Funit = save(u1, u2, Follow)
  def unfollow(u1: ID, u2: ID): Funit = remove(u1, u2)
  def block(u1: ID, u2: ID): Funit = save(u1, u2, Block)
  def unblock(u1: ID, u2: ID): Funit = remove(u1, u2)

  def unfollowAll(u1: ID): Funit = coll.delete.one($doc("u1" -> u1)).void

  private def save(u1: ID, u2: ID, relation: Relation): Funit = coll.update.one(
    $id(makeId(u1, u2)),
    $doc("u1" -> u1, "u2" -> u2, "r" -> relation),
    upsert = true
  ).void

  def remove(u1: ID, u2: ID): Funit = coll.delete.one($id(makeId(u1, u2))).void

  def drop(userId: ID, relation: Relation, nb: Int) =
    coll.find(
      $doc("u1" -> userId, "r" -> relation),
      $doc("_id" -> true).some
    )
      .list[Bdoc](nb).map {
        _.flatMap { _.string("_id") }
      } flatMap { ids =>
        coll.delete.one($inIds(ids)).void
      }
}

object RelationRepo {

  def makeId(u1: ID, u2: ID) = s"$u1/$u2"
}
