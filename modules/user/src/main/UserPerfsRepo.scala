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

  def idsMap[U: UserIdOf](
      u: Seq[U],
      readPref: ReadPref
  ): Fu[Map[UserId, UserPerfs]] =
    coll.idsMap[UserPerfs, UserId](u.map(_.id), none, readPref)(_.id)

  def idsMap[U: UserIdOf](u: Seq[U], pt: PerfType): Fu[Map[UserId, Perf]] =
    given BSONDocumentReader[(UserId, Perf)] = UserPerfs.idPerfReader(pt)
    coll
      .find($inIds(u.map(_.id)), $doc(pt.key.value -> true).some)
      .cursor[(UserId, Perf)](ReadPref.sec)
      .listAll()
      .map(_.toMap)

  def perfsOf[U: UserIdOf](u: U): Fu[UserPerfs] =
    coll.byId[UserPerfs](u.id).dmap(_ | UserPerfs.default(u.id))

  def perfsOf[U: UserIdOf](us: PairOf[U], readPref: ReadPref): Fu[PairOf[UserPerfs]] = us match
    case (x, y) =>
      idsMap(List(x, y), readPref).dmap: ps =>
        ps.getOrElse(x.id, UserPerfs.default(x.id)) -> ps.getOrElse(y.id, UserPerfs.default(y.id))

  def withPerfs(u: User): Fu[User.WithPerfs] =
    perfsOf(u).dmap(User.WithPerfs(u, _))

  def withPerfs(us: PairOf[User], readPref: ReadPref): Fu[PairOf[User.WithPerfs]] =
    perfsOf(us, readPref).dmap: (x, y) =>
      User.WithPerfs(us._1, y) -> User.WithPerfs(us._2, x)

  def withPerfs(us: Seq[User], readPref: ReadPref = _.sec): Fu[List[User.WithPerfs]] =
    idsMap(us, readPref).map: perfs =>
      us.view.map(u => User.WithPerfs(u, perfs.get(u.id))).toList

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

  def perfOptionOf[U: UserIdOf](u: U, perfType: PerfType): Fu[Option[Perf]] =
    coll
      .find(
        $id(u.id),
        $doc(perfType.key.value -> true).some
      )
      .one[Bdoc]
      .dmap:
        _.flatMap { docPerf(_, perfType) }

  def perfOf[U: UserIdOf](u: U, perfType: PerfType): Fu[Perf] =
    perfOptionOf(u, perfType).dmap(_ | Perf.default)

  def usingPerfOf[A, U: UserIdOf](u: U, perfType: PerfType)(f: Perf ?=> Fu[A]): Fu[A] =
    perfOf(u, perfType)
      .flatMap: perf =>
        given Perf = perf
        f

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

  def perfsOf(ids: Iterable[UserId]): Fu[Map[UserId, UserPerfs]] = ids.nonEmpty.so:
    coll.find($inIds(ids)).cursor[UserPerfs]().listAll().map(_.mapBy(_.id))

  def withPerf(users: List[User], perfType: PerfType): Fu[List[User.WithPerf]] =
    perfOf(users.map(_.id), perfType).map: perfs =>
      users.map(u => u.withPerf(perfs.getOrElse(u.id, Perf.default)))

  def withPerf(user: User, perfType: PerfType): Fu[User.WithPerf] =
    perfOf(user.id, perfType).dmap(user.withPerf)

  def dubiousPuzzle(id: UserId, puzzle: Perf): Fu[Boolean] =
    if puzzle.glicko.rating < 2500
    then fuFalse
    else
      perfOptionOf(id, PerfType.Standard).map:
        _.fold(true)(UserPerfs.dubiousPuzzle(puzzle, _))

  object aggregate:
    val lookup = $doc:
      "$lookup" -> $doc(
        "from"         -> coll.name,
        "localField"   -> "_id",
        "foreignField" -> "_id",
        "as"           -> "perfs"
      )
    def read[U: UserIdOf](root: Bdoc, u: U) =
      root.getAsOpt[List[UserPerfs]]("perfs").flatMap(_.headOption).getOrElse(UserPerfs.default(u.id))
