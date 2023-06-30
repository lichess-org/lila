package lila.user

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.rating.{ Perf, PerfType }
import lila.rating.Glicko

final class UserPerfsRepo(coll: Coll)(using Executor):

  import UserPerfs.given

  def glickoField(perf: Perf.Key) = s"$perf.gl"

  def byId[U: UserIdOf](u: U): Fu[UserPerfs] =
    coll.byId[UserPerfs](u.id).dmap(_ | UserPerfs.default(u.id))

  def withPerfs(u: User): Fu[User.WithPerfs] =
    coll
      .byId[UserPerfs](u.id)
      .dmap: perfs =>
        User.WithPerfs(u, perfs | UserPerfs.default(u.id))

  def setPerfs(user: User, perfs: UserPerfs, prev: UserPerfs)(using wr: BSONHandler[Perf]) =
    val diff = for
      pt <- PerfType.all
      if perfs(pt).nb != prev(pt).nb
      bson <- wr.writeOpt(perfs(pt))
    yield BSONElement(pt.key.value, bson)
    diff.nonEmpty so coll.update
      .one($id(user.id), $doc("$set" -> $doc(diff*)))
      .void

  def setManagedUserInitialPerfs(id: UserId) =
    coll.update.one($id(id), UserPerfs.defaultManaged(id), upsert = true).void
  def setBotInitialPerfs(id: UserId) =
    coll.update.one($id(id), UserPerfs.defaultBot(id), upsert = true).void

  def setPerf(userId: UserId, pt: PerfType, perf: Perf) =
    coll.updateField($id(userId), s"${pt.key}", perf).void

  def sortPerfDesc(perf: String) = $sort desc s"$perf.gl.r"

  def glicko(userId: UserId, perfType: PerfType): Fu[Glicko] =
    coll
      .find($id(userId), $doc(s"${perfType.key}.gl" -> true).some)
      .one[Bdoc]
      .dmap:
        _.flatMap(_ child perfType.key.value)
          .flatMap(_.getAsOpt[Glicko]("gl")) | Glicko.default

  def addStormRun  = addStormLikeRun("storm")
  def addRacerRun  = addStormLikeRun("racer")
  def addStreakRun = addStormLikeRun("streak")

  private def addStormLikeRun(field: String)(userId: UserId, score: Int): Funit =
    coll.update
      .one(
        $id(userId),
        $inc(s"perfs.$field.runs" -> 1) ++
          $doc("$max" -> $doc(s"perfs.$field.score" -> score))
      )
      .void

  private def docPerf(doc: Bdoc, perfType: PerfType): Option[Perf] =
    doc.getAsOpt[Perf](perfType.key.value)

  def perfOf(id: UserId, perfType: PerfType): Fu[Option[Perf]] =
    coll
      .find(
        $id(id),
        $doc(perfType.key.value -> true).some
      )
      .one[Bdoc]
      .dmap:
        _.flatMap { docPerf(_, perfType) }

  def perfOfDefault(id: UserId, perfType: PerfType): Fu[Perf] =
    perfOf(id, perfType).dmap(_ | Perf.default)

  def perfOf(ids: Iterable[UserId], perfType: PerfType): Fu[Map[UserId, Perf]] = ids.nonEmpty.so:
    coll
      .find(
        $inIds(ids),
        $doc(perfType.key.value -> true).some
      )
      .cursor[Bdoc]()
      .listAll()
      .map: docs =>
        for
          doc <- docs
          id  <- doc.getAsOpt[UserId]("_id")
          perf = docPerf(doc, perfType) | Perf.default
        yield id -> perf
      .dmap(_.toMap)

  def withPerf(users: List[User], perfType: PerfType): Fu[List[User.WithPerf]] =
    perfOf(users.map(_.id), perfType).map: perfs =>
      users.map(u => User.WithPerf(u, perfs.getOrElse(u.id, Perf.default)))

  def dubiousPuzzle(id: UserId, puzzle: Perf): Fu[Boolean] =
    if puzzle.glicko.rating < 2500
    then fuFalse
    else
      perfOf(id, PerfType.Standard).map:
        _.fold(true)(UserPerfs.dubiousPuzzle(puzzle, _))
