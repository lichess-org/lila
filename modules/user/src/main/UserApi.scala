package lila.user

import lila.rating.{ Perf, PerfType }
import lila.memo.CacheApi
import lila.common.LightUser

final class UserApi(userRepo: UserRepo, perfsRepo: UserPerfsRepo, cacheApi: CacheApi)(using Executor):

  private type GameUserIds = PairOf[Option[UserId]]

  object gamePlayers:
    def apply(userIds: PairOf[Option[UserId]], perfType: PerfType): Fu[GamePlayers] =
      gamePlayersCache.get(userIds -> perfType)

    // hit by game rounds
    // TODO aggregation to save 1 req? maybe not
    private type PlayersKey  = (GameUserIds, PerfType)
    private type GamePlayers = PairOf[Option[User.WithPerf]]
    private val gamePlayersCache = cacheApi[PlayersKey, GamePlayers](4096, "user.perf.pair"):
      _.expireAfterWrite(3 seconds).buildAsyncFuture:
        case ((x, y), perfType) =>
          for
            (x, y) <- userRepo.pair(x, y)
            perfs  <- perfsRepo.perfOf(List(x, y).flatten.map(_.id), perfType)
            make = (u: Option[User]) => u.map(u => User.WithPerf(u, perfs.getOrElse(u.id, Perf.default)))
          yield make(x) -> make(y)

  def withPerfs(u: User): Fu[User.WithPerfs]                    = perfsRepo.withPerfs(u)
  def withPerfs[U: UserIdOf](id: U): Fu[Option[User.WithPerfs]] = userRepo.withPerfs(id)

  def pairWithPerfs(userIds: GameUserIds): Fu[PairOf[Option[User.WithPerfs]]] =
    for
      (x, y) <- userRepo.pair.tupled(userIds)
      perfs  <- perfsRepo.perfsOf(List(x, y).flatten.map(_.id))
      make = (u: Option[User]) =>
        u.map(u => User.WithPerfs(u, perfs.getOrElse(u.id, UserPerfs.default(u.id))))
    yield make(x) -> make(y)

  def byIdOrGhostWithPerf(id: UserId, pt: PerfType): Fu[Option[Either[LightUser.Ghost, (User, Perf)]]] =
    userRepo
      .byIdOrGhost(id)
      .flatMapz:
        case Left(g)  => fuccess(Left(g).some)
        case Right(u) => perfsRepo.perfOf(u.id, pt).dmap(p => Right(u -> p).some)

  def setBot(user: User): Funit =
    if user.count.game > 0
    then fufail(lila.base.LilaInvalid("You already have games played. Make a new account."))
    else
      userRepo.addTitle(user.id, Title.BOT) >>
        perfsRepo.setBotInitialPerfs(user.id)
