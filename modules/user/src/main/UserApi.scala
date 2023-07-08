package lila.user

import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.bson.BSONNull
import akka.stream.scaladsl.*

import lila.rating.{ Perf, PerfType }
import lila.memo.CacheApi
import lila.common.LightUser
import lila.db.dsl.{ *, given }
import lila.db.BSON.Reader
import lila.rating.Glicko
import lila.user.User.userHandler
import chess.ByColor

final class UserApi(userRepo: UserRepo, perfsRepo: UserPerfsRepo, cacheApi: CacheApi)(using
    Executor,
    akka.stream.Materializer
):

  private type GameUserIds = PairOf[Option[UserId]]

  object gamePlayers:
    private type PlayersKey = (GameUserIds, PerfType)

    // hit by game rounds
    def apply(userIds: PairOf[Option[UserId]], perfType: PerfType): Fu[GameUsers] =
      cache.get(userIds -> perfType)

    def apply(userIds: ByColor[Option[UserId]], perfType: PerfType): Fu[ByColor[Option[User.WithPerf]]] =
      apply(userIds.toPair, perfType).dmap((w, b) => ByColor(w, b))

    def loggedIn(ids: PairOf[UserId], perfType: PerfType): Fu[Option[PairOf[User.WithPerf]]] =
      cache
        .get((ids._1.some, ids._2.some) -> perfType)
        .dmap(_.tupled)

    private val cache = cacheApi[PlayersKey, GameUsers](4096, "user.perf.pair"):
      _.expireAfterWrite(3 seconds).buildAsyncFuture:
        case ((x, y), perfType) =>
          listWithPerf(List(x, y).flatten, perfType).map: users =>
            (x.flatMap(id => users.find(_.id == id)), y.flatMap(id => users.find(_.id == id)))

  def withPerfs(u: User): Fu[User.WithPerfs] = perfsRepo.withPerfs(u)

  def withPerfs[U: UserIdOf](id: U): Fu[Option[User.WithPerfs]] =
    userRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List:
          PipelineOperator(perfsRepo.aggregate.lookup)
      .map: docO =>
        for
          doc  <- docO
          user <- doc.asOpt[User]
          perfs = perfsRepo.aggregate.readFirst(doc, user)
        yield User.WithPerfs(user, perfs)

  def enabledWithPerfs[U: UserIdOf](id: U): Fu[Option[User.WithPerfs]] =
    withPerfs(id).dmap(_.filter(_.enabled.yes))

  def listWithPerfs[U: UserIdOf](us: List[U], readPref: ReadPref = _.sec): Fu[List[User.WithPerfs]] =
    us.nonEmpty.so:
      val ids = us.map(_.id)
      userRepo.coll
        .aggregateList(Int.MaxValue, readPref): framework =>
          import framework.*
          Match($inIds(ids)) -> List(
            PipelineOperator(perfsRepo.aggregate.lookup),
            AddFields($sort.orderField(ids)),
            Sort(Ascending("_order"))
          )
        .map: docs =>
          for
            doc  <- docs
            user <- doc.asOpt[User]
            perfs = perfsRepo.aggregate.readFirst(doc, user)
          yield User.WithPerfs(user, perfs)

  def withPerf[U: UserIdOf](id: U, pt: PerfType): Fu[Option[User.WithPerf]] =
    userRepo.byId(id).flatMapz(perfsRepo.withPerf(_, pt).dmap(some))

  def pairWithPerfs(userIds: GameUserIds): Fu[PairOf[Option[User.WithPerfs]]] =
    val (x, y) = userIds
    listWithPerfs(List(x, y).flatten).map: users =>
      (x.flatMap(id => users.find(_.id == id)), y.flatMap(id => users.find(_.id == id)))

  def listWithPerf[U: UserIdOf](
      us: List[U],
      pt: PerfType,
      readPref: ReadPref = _.sec
  ): Fu[List[User.WithPerf]] = us.nonEmpty.so:
    val ids = us.map(_.id)
    userRepo.coll
      .aggregateList(Int.MaxValue, readPref): framework =>
        import framework.*
        Match($inIds(ids)) -> List(
          PipelineOperator(perfsRepo.aggregate.lookup(pt)),
          AddFields($sort.orderField(ids)),
          Sort(Ascending("_order"))
        )
      .map: docs =>
        for
          doc  <- docs
          user <- doc.asOpt[User]
          perf = perfsRepo.aggregate.readFirst(doc, pt)
        yield User.WithPerf(user, perf)

  def pairWithPerf(userIds: GameUserIds, pt: PerfType): Fu[PairOf[Option[User.WithPerf]]] =
    val (x, y) = userIds
    listWithPerf(List(x, y).flatten, pt).map: users =>
      (x.flatMap(id => users.find(_.id == id)), y.flatMap(id => users.find(_.id == id)))

  def byIdOrGhostWithPerf(id: UserId, pt: PerfType): Fu[Option[LightUser.Ghost | User.WithPerf]] =
    userRepo
      .byIdOrGhost(id)
      .flatMapz:
        case Left(g)  => fuccess(g.some)
        case Right(u) => perfsRepo.perfOf(u.id, pt).dmap(p => u.withPerf(p).some)

  def setBot(user: User): Funit =
    if user.count.game > 0
    then fufail(lila.base.LilaInvalid("You already have games played. Make a new account."))
    else
      userRepo.addTitle(user.id, Title.BOT) >>
        perfsRepo.setBotInitialPerfs(user.id)

  def botsByIdsStream(ids: Iterable[UserId], nb: Option[Int]): Source[User.WithPerfs, ?] =
    userRepo.coll
      .find($inIds(ids) ++ userRepo.botSelect(true))
      .cursor[User](ReadPref.priTemp)
      .documentSource(nb | Int.MaxValue)
      .grouped(40)
      .mapAsync(1)(perfsRepo.withPerfs(_))
      .throttle(1, 1 second)
      .mapConcat(identity)

  // expensive, send to secondary
  def byIdsSortRatingNoBot(ids: Iterable[UserId], nb: Int): Fu[List[User.WithPerfs]] =
    userRepo.coll
      .aggregateList(nb, _.sec): framework =>
        import framework.*
        import User.{ BSONFields as F }
        Match(
          $doc(
            F.enabled -> true,
            F.marks $nin List(UserMark.Engine.key, UserMark.Boost.key)
          ) ++ $inIds(ids) ++ userRepo.botSelect(false)
        ) -> List(
          Project($id(true)),
          Group(BSONNull)("ids" -> PushField("_id")),
          PipelineOperator:
            $lookup.simple(
              from = perfsRepo.coll,
              as = "perfs",
              local = "ids",
              foreign = "_id"
            )
          ,
          Match($doc("perfs.0.standard.gl.d" $lt Glicko.provisionalDeviation)),
          Project($doc("perfs" -> true)),
          UnwindField("perfs"),
          Sort(Descending("perfs.standard.gl.r")),
          Limit(nb),
          PipelineOperator:
            $lookup.simple(
              from = userRepo.coll,
              as = "user",
              local = "perfs._id",
              foreign = "_id"
            )
          ,
          UnwindField("user")
        )
      .map: docs =>
        for
          doc  <- docs
          user <- doc.getAsOpt[User]("user")
          perfs = perfsRepo.aggregate.readOne(doc, user)
        yield User.WithPerfs(user, perfs)
