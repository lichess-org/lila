package lila.user

import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.bson.*
import akka.stream.scaladsl.*

import lila.rating.{ Perf, PerfType }
import lila.memo.CacheApi
import lila.common.{ LightUser, NormalizedEmailAddress }
import lila.db.dsl.{ *, given }
import lila.db.BSON.Reader
import lila.rating.Glicko
import lila.user.User.userHandler
import chess.ByColor

final class UserApi(userRepo: UserRepo, perfsRepo: UserPerfsRepo, cacheApi: CacheApi)(using
    Executor,
    akka.stream.Materializer
):

  // hit by game rounds
  object gamePlayers:
    private type PlayersKey = (PairOf[Option[UserId]], PerfType)

    def apply(userIds: ByColor[Option[UserId]], perfType: PerfType): Fu[GameUsers] =
      cache.get(userIds.toPair -> perfType)

    def noCache(userIds: ByColor[Option[UserId]], perfType: PerfType): Fu[GameUsers] =
      fetch(userIds.toPair, perfType)

    def loggedIn(
        ids: ByColor[UserId],
        perfType: PerfType,
        useCache: Boolean = true
    ): Fu[Option[ByColor[User.WithPerf]]] =
      val users =
        if useCache then apply(ids.map(some), perfType)
        else fetch(ids.map(some).toPair, perfType)
      users.map:
        case ByColor(Some(x), Some(y)) => ByColor(x, y).some
        case _                         => none

    private[UserApi] val cache = cacheApi[PlayersKey, GameUsers](1024, "user.perf.pair"):
      _.expireAfterWrite(3 seconds).buildAsyncFuture(fetch)

    private def fetch(userIds: PairOf[Option[UserId]], perfType: PerfType): Fu[GameUsers] =
      val (x, y) = userIds
      listWithPerf(List(x, y).flatten, perfType, _.pri).map: users =>
        ByColor(x, y).map(_.flatMap(id => users.find(_.id == id)))

  def updatePerfs(ups: ByColor[(UserPerfs, UserPerfs)], gamePerfType: PerfType) =
    import lila.memo.CacheApi.invalidate
    ups.all.map(perfsRepo.updatePerfs).parallel andDo
      gamePlayers.cache.invalidate(ups.map(_._1.id.some).toPair -> gamePerfType)

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

  def enabledWithPerf[U: UserIdOf](id: U, perfType: PerfType): Fu[Option[User.WithPerf]] =
    withPerf(id, perfType).dmap(_.filter(_.user.enabled.yes))

  def listWithPerfs[U: UserIdOf](us: List[U]): Fu[List[User.WithPerfs]] =
    us.nonEmpty.so:
      val ids = us.map(_.id)
      userRepo.coll
        .aggregateList(Int.MaxValue, _.autoTemp(ids)): framework =>
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

  def pairWithPerfs(userIds: ByColor[Option[UserId]]): Fu[ByColor[Option[User.WithPerfs]]] =
    listWithPerfs(userIds.flatten).map: users =>
      userIds.map(_.flatMap(id => users.find(_.id == id)))

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

  def pairWithPerf(userIds: ByColor[Option[UserId]], pt: PerfType): Fu[ByColor[Option[User.WithPerf]]] =
    listWithPerf(userIds.flatten, pt).map: users =>
      userIds.map(_.flatMap(id => users.find(_.id == id)))

  def byIdOrGhostWithPerf(id: UserId, pt: PerfType): Fu[Option[LightUser.Ghost | User.WithPerf]] =
    userRepo
      .byIdOrGhost(id)
      .flatMapz:
        case Left(g)  => fuccess(g.some)
        case Right(u) => perfsRepo.perfOf(u.id, pt).dmap(p => u.withPerf(p).some)

  def withEmails[U: UserIdOf](users: List[U])(using r: BSONHandler[User]): Fu[List[User.WithEmails]] = for
    perfs <- perfsRepo.idsMap(users, _.sec)
    users <- userRepo.coll
      .list[Bdoc]($inIds(users.map(_.id)), _.priTemp)
      .map: docs =>
        for
          doc  <- docs
          user <- r readOpt doc
        yield User.WithEmails(
          User.WithPerfs(user, perfs.get(user.id)),
          User.Emails(
            current = userRepo.anyEmail(doc),
            previous = doc.getAsOpt[NormalizedEmailAddress](User.BSONFields.prevEmail)
          )
        )
  yield users

  def withEmails[U: UserIdOf](u: U)(using r: BSONHandler[User]): Fu[Option[User.WithEmails]] =
    withEmails(List(u)).map(_.headOption)

  def setBot(user: User): Funit =
    if user.count.game > 0
    then fufail(lila.base.LilaInvalid("You already have games played. Make a new account."))
    else
      userRepo.addTitle(user.id, Title.BOT) >>
        userRepo.setRoles(user.id, Nil) >>
        perfsRepo.setBotInitialPerfs(user.id)

  def visibleBotsByIds(ids: Iterable[UserId], max: Int = 200): Fu[List[User.WithPerfs]] =
    userRepo.coll
      .aggregateList(max, _.priTemp): framework =>
        import framework.*
        Match($inIds(ids) ++ userRepo.botWithBioSelect) -> List(
          Sort(Descending(User.BSONFields.roles), Descending(User.BSONFields.playTimeTotal)),
          Limit(max),
          PipelineOperator(perfsRepo.aggregate.lookup)
        )
      .map: docs =>
        for
          doc  <- docs
          user <- doc.asOpt[User]
          perfs = perfsRepo.aggregate.readFirst(doc, user)
        yield User.WithPerfs(user, perfs)

  // expensive, send to secondary
  def byIdsSortRatingNoBot(ids: Iterable[UserId], nb: Int): Fu[List[User.WithPerfs]] =
    perfsRepo.coll
      .aggregateList(nb, _.sec): framework =>
        import framework.*
        import User.{ BSONFields as F }
        Match(
          $inIds(ids) ++ $doc("standard.gl.d" $lt Glicko.provisionalDeviation)
        ) -> List(
          Sort(Descending("standard.gl.r")),
          Limit(nb * 5),
          PipelineOperator:
            $lookup.simple(
              from = userRepo.coll,
              as = "user",
              local = "_id",
              foreign = "_id"
            )
          ,
          UnwindField("user"),
          Match:
            $doc(
              s"user.${F.enabled}" -> true,
              s"user.${F.marks}" $nin List(UserMark.Engine.key, UserMark.Boost.key),
              s"user.${F.title}" $ne Title.BOT
            )
          ,
          Limit(nb)
        )
      .map: docs =>
        for
          doc  <- docs
          user <- doc.getAsOpt[User]("user")
          perfs = perfsRepo.aggregate.readFrom(doc, user)
        yield User.WithPerfs(user, perfs)
