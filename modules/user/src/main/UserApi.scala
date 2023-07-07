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

final class UserApi(userRepo: UserRepo, perfsRepo: UserPerfsRepo, cacheApi: CacheApi)(using
    Executor,
    akka.stream.Materializer
):

  private type GameUserIds = PairOf[Option[UserId]]

  object gamePlayers:
    private type PlayersKey  = (GameUserIds, PerfType)
    private type GamePlayers = PairOf[Option[User.WithPerf]]

    // hit by game rounds
    // TODO aggregation to save 1 req? maybe not
    def apply(userIds: PairOf[Option[UserId]], perfType: PerfType): Fu[GamePlayers] =
      gamePlayersCache.get(userIds -> perfType)

    def loggedIn(ids: PairOf[UserId], perfType: PerfType): Fu[Option[PairOf[User.WithPerf]]] =
      gamePlayersCache
        .get((ids._1.some, ids._2.some) -> perfType)
        .dmap(_.tupled)

    private val gamePlayersCache = cacheApi[PlayersKey, GamePlayers](4096, "user.perf.pair"):
      _.expireAfterWrite(3 seconds).buildAsyncFuture:
        case ((x, y), perfType) =>
          for
            (x, y) <- userRepo.pair(x, y)
            perfs  <- perfsRepo.perfOf(List(x, y).flatten.map(_.id), perfType)
            make = (u: Option[User]) => u.map(u => u.withPerf(perfs.getOrElse(u.id, Perf.default)))
          yield make(x) -> make(y)

  def withPerfs(u: User): Fu[User.WithPerfs]                    = perfsRepo.withPerfs(u)
  def withPerfs[U: UserIdOf](id: U): Fu[Option[User.WithPerfs]] = userRepo.withPerfs(id)
  def enabledWithPerfs[U: UserIdOf](id: U): Fu[Option[User.WithPerfs]] =
    withPerfs(id).dmap(_.filter(_.enabled.yes))

  def listWithPerfs[U: UserIdOf](ids: List[U], readPref: ReadPref = _.sec): Fu[List[User.WithPerfs]] = for
    users <- userRepo.byIds(ids, readPref)
    perfs <- perfsRepo.idsMap(ids, readPref)
  yield users.map: user =>
    User.WithPerfs(user, perfs.get(user.id))

  def withPerf[U: UserIdOf](id: U, pt: PerfType): Fu[Option[User.WithPerf]] =
    userRepo.byId(id).flatMapz(perfsRepo.withPerf(_, pt).dmap(some))

  def pairWithPerfs(userIds: GameUserIds): Fu[PairOf[Option[User.WithPerfs]]] = for
    (x, y) <- userRepo.pair.tupled(userIds)
    perfs  <- perfsRepo.perfsOf(List(x, y).flatten.map(_.id))
    make = (u: Option[User]) => u.map(u => User.WithPerfs(u, perfs.get(u.id)))
  yield make(x) -> make(y)

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
