package lila.user

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.rating.{ Perf, PerfType }
import lila.rating.Glicko

final class UserPerfsRepo(private[user] val coll: Coll)(using Executor):

  import UserPerfs.given

  def glickoField(perf: Perf.Key) = s"$perf.gl"

  def byId[U: UserIdOf](u: U): Fu[UserPerfs] =
    coll.byId[UserPerfs](u.id).dmap(_ | UserPerfs.default(u.id))

  def idsMap[U: UserIdOf](
      u: Seq[U],
      readPref: ReadPref
  ): Fu[Map[UserId, UserPerfs]] =
    coll.idsMap[UserPerfs, UserId](u.map(_.id), none, readPref)(_.id)

  def idsMap[U: UserIdOf](u: Seq[U], pt: PerfType, readPref: ReadPref): Fu[Map[UserId, Perf]] =
    given BSONDocumentReader[(UserId, Perf)] = UserPerfs.idPerfReader(pt)
    coll
      .find($inIds(u.map(_.id)), $doc(pt.key.value -> true).some)
      .cursor[(UserId, Perf)](readPref)
      .listAll()
      .map(_.toMap)

  def perfsOf[U: UserIdOf](u: U): Fu[UserPerfs] =
    coll.byId[UserPerfs](u.id).dmap(_ | UserPerfs.default(u.id))

  def perfsOf[U: UserIdOf](us: PairOf[U], readPref: ReadPref): Fu[PairOf[UserPerfs]] =
    val (x, y) = us
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

  def updatePerfs(prev: UserPerfs, cur: UserPerfs)(using wr: BSONHandler[Perf]) =
    val diff = for
      pt <- PerfType.all
      if cur(pt).nb != prev(pt).nb
      bson <- wr.writeOpt(cur(pt))
    yield BSONElement(pt.key.value, bson)
    diff.nonEmpty so
      coll.update.one($id(cur.id), $doc("$set" -> $doc(diff*)), upsert = true).void

  def setManagedUserInitialPerfs(id: UserId) =
    coll.update.one($id(id), UserPerfs.defaultManaged(id), upsert = true).void
  def setBotInitialPerfs(id: UserId) =
    coll.update.one($id(id), UserPerfs.defaultBot(id), upsert = true).void

  def setPerf(userId: UserId, pt: PerfType, perf: Perf) =
    coll.update.one($id(userId), $set(pt.key.value -> perf), upsert = true).void

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
        $inc(s"$field.runs" -> 1) ++
          $doc("$max" -> $doc(s"$field.score" -> score)),
        upsert = true
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
          doc  <- docs
          id   <- doc.getAsOpt[UserId]("_id")
          perf <- docPerf(doc, perfType)
        yield id -> perf
      .map: pairs =>
        val h = pairs.toMap
        ids.map(id => id -> h.getOrElse(id, Perf.default)).toMap

  def perfsOf(ids: Iterable[UserId]): Fu[Map[UserId, UserPerfs]] = ids.nonEmpty.so:
    coll.find($inIds(ids)).cursor[UserPerfs]().listAll().map(_.mapBy(_.id))

  def withPerf(users: List[User], perfType: PerfType): Fu[List[User.WithPerf]] =
    perfOf(users.map(_.id), perfType).map: perfs =>
      users.map(u => u.withPerf(perfs.getOrElse(u.id, Perf.default)))

  def withPerf(user: User, perfType: PerfType): Fu[User.WithPerf] =
    perfOf(user.id, perfType).dmap(user.withPerf)

  def withPerf(us: PairOf[User], perfType: PerfType, readPref: ReadPref): Fu[PairOf[User.WithPerf]] =
    perfOf(us, perfType, readPref).dmap: (x, y) =>
      User.WithPerf(us._1, x) -> User.WithPerf(us._2, y)

  def perfOf[U: UserIdOf](us: PairOf[U], perfType: PerfType, readPref: ReadPref): Fu[PairOf[Perf]] =
    val (x, y) = us
    idsMap(List(x, y), perfType, readPref).dmap: ps =>
      ps.getOrElse(x.id, Perf.default) -> ps.getOrElse(y.id, Perf.default)

  def dubiousPuzzle(id: UserId, puzzle: Perf): Fu[Boolean] =
    if puzzle.glicko.rating < 2500
    then fuFalse
    else
      perfOptionOf(id, PerfType.Standard).map:
        _.forall(UserPerfs.dubiousPuzzle(puzzle, _))

  object aggregate:
    val lookup = $lookup.simple(coll, "perfs", "_id", "_id")

    def lookup(pt: PerfType): Bdoc =
      val pipe = List($doc("$project" -> $doc(pt.key.value -> true)))
      $lookup.pipeline(coll, "perfs", "_id", "_id", pipe)

    def readFirst[U: UserIdOf](root: Bdoc, u: U): UserPerfs =
      root.getAsOpt[List[UserPerfs]]("perfs").flatMap(_.headOption).getOrElse(UserPerfs.default(u.id))

    def readFirst[U: UserIdOf](root: Bdoc, pt: PerfType): Perf = (for
      perfs <- root.getAsOpt[List[Bdoc]]("perfs")
      perfs <- perfs.headOption
      perf  <- perfs.getAsOpt[Perf](pt.key.value)
    yield perf).getOrElse(Perf.default)

    def readFrom[U: UserIdOf](doc: Bdoc, u: U): UserPerfs =
      doc.asOpt[UserPerfs].getOrElse(UserPerfs.default(u.id))
