package lila.relation

import reactivemongo.api.bson.*

import lila.core.relation.Relation.{ Block, Follow }
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }

final private class RelationRepo(colls: Colls, userRepo: lila.core.user.UserRepo)(using Executor):

  val coll = colls.relation

  def following(userId: UserId): Fu[Set[UserId]] = relating(userId, Follow)

  def blockers(userId: UserId): Fu[Set[UserId]] = relaters(userId, Block)
  def blocking(userId: UserId): Fu[Set[UserId]] = relating(userId, Block)

  def freshFollowersFromSecondary(userId: UserId): Fu[List[UserId]] =
    coll
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match(bdoc("u2" -> userId, "r" -> Follow)) -> List(
          PipelineOperator(
            lookup.simple(
              from = userRepo.coll,
              as = "follower",
              local = "u1",
              foreign = "_id",
              pipe = List(
                bdoc("$match" -> bdoc("seenAt".gt(nowInstant.minusDays(10)))),
                bdoc("$project" -> bid(true))
              )
            )
          ),
          Match("follower".neq(barr())),
          Group(BSONNull)("ids" -> PushField("u1"))
        )
      .map(~_.flatMap(_.getAsOpt[List[UserId]]("ids")))

  def followingLike(userId: UserId, term: UserSearch): Fu[List[UserId]] =
    coll.secondary.distinctEasy[UserId, List](
      "u2",
      bdoc(
        "u1" -> userId,
        "u2".regexStart(term.value),
        "r" -> Follow
      )
    )

  private def relaters(
      userId: UserId,
      relation: Relation,
      readPref: ReadPref = _.pri
  ): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](
      "u1",
      bdoc("u2" -> userId, "r" -> relation),
      readPref
    )

  private def relating(userId: UserId, relation: Relation): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](
      "u2",
      bdoc("u1" -> userId, "r" -> relation)
    )

  def follow(u1: UserId, u2: UserId): Funit = save(u1, u2, Follow)
  def unfollow(u1: UserId, u2: UserId): Funit = remove(u1, u2)
  def block(u1: UserId, u2: UserId): Funit = save(u1, u2, Block)
  def unblock(u1: UserId, u2: UserId): Funit = remove(u1, u2)

  def unfollowMany(u1: UserId, u2s: Iterable[UserId]): Funit =
    coll.delete.one(inIds(u2s.map { makeId(u1, _) })).void

  def removeAllRelationsFrom(u1: UserId): Funit = coll.delete.one(bdoc("u1" -> u1)).void

  def removeAllFollowers(u2: UserId): Funit = coll.delete.one(bdoc("u2" -> u2, "r" -> Follow)).void

  private def save(u1: UserId, u2: UserId, relation: Relation): Funit =
    coll.update
      .one(
        bid(makeId(u1, u2)),
        bdoc("u1" -> u1, "u2" -> u2, "r" -> relation),
        upsert = true
      )
      .void

  def remove(u1: UserId, u2: UserId): Funit = coll.delete.one(bid(makeId(u1, u2))).void

  def drop(userId: UserId, relation: Relation, nb: Int) =
    coll
      .find(
        bdoc("u1" -> userId, "r" -> relation),
        bdoc("_id" -> true).some
      )
      .cursor[Bdoc]()
      .list(nb)
      .dmap:
        _.flatMap { _.string("_id") }
      .flatMap: ids =>
        coll.delete.one(inIds(ids)).void

  def filterBlocked(by: UserId, candidates: Iterable[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("u2", bdoc("u2".in(candidates), "u1" -> by, "r" -> Block))

  def filterBlocking(candidates: Iterable[UserId], blocked: UserId): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("u1", bdoc("u1".in(candidates), "u2" -> blocked, "r" -> Block))
