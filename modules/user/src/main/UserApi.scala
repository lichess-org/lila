package lila.user

import chess.{ ByColor, PlayerTitle }
import chess.IntRating
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.core.LightUser
import lila.core.email.NormalizedEmailAddress
import lila.core.lilaism.LilaInvalid
import lila.core.perf.{ UserPerfs, UserWithPerfs }
import lila.core.user.{ GameUsers, UserMark, WithEmails, WithPerf, KidMode, SetKidMode }
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.rating.PerfType
import lila.user.BSONHandlers.userHandler

final class UserApi(userRepo: UserRepo, perfsRepo: UserPerfsRepo, cacheApi: CacheApi)(using
    Executor
) extends lila.core.user.UserApi:

  export userRepo.{
    byId,
    byIds,
    byIdAs,
    me,
    email,
    emailOrPrevious,
    pair,
    getTitle,
    enabledById,
    enabledByIds,
    createdAtById,
    isEnabled,
    filterClosedOrInactiveIds,
    isKid,
    langOf,
    isBot,
    isTroll,
    isManaged,
    filterDisabled,
    countEngines,
    filterEngines,
    firstGetsWhite,
    incColor,
    mustPlayAsColor,
    userIdsWithRoles,
    containsDisabled,
    containsEngine,
    filterLame,
    filterExists,
    filterKid,
    incToints,
    setPlan,
    filterByEnabledPatrons,
    isCreatedSince,
    accountAge
  }
  export perfsRepo.{
    perfOf,
    perfsOf,
    setPerf,
    dubiousPuzzle,
    glicko,
    withPerf,
    usingPerfOf,
    perfOptionOf,
    addPuzRun,
    withPerfs
  }
  export gamePlayers.{ apply as gamePlayersAny, loggedIn as gamePlayersLoggedIn }

  // hit by game rounds
  object gamePlayers:
    private type PlayersKey = (PairOf[Option[UserId]], PerfKey)

    def apply(userIds: ByColor[Option[UserId]], perf: PerfKey): Fu[GameUsers] =
      cache.get(userIds.toPair -> perf)

    def loggedIn(
        ids: ByColor[UserId],
        perf: PerfKey,
        useCache: Boolean = true
    ): Fu[Option[ByColor[WithPerf]]] =
      val users =
        if useCache then apply(ids.map(some), perf)
        else fetch(_.pri)(ids.map(some).toPair, perf)
      users.map:
        case ByColor(Some(x), Some(y)) => ByColor(x, y).some
        case _ => none

    def analysis(game: Game): Fu[GameUsers] = fetch(_.sec)(game.userIdPair.toPair, game.perfKey)

    private[UserApi] val cache = cacheApi[PlayersKey, GameUsers](1024, "user.perf.pair"):
      _.expireAfterWrite(3.seconds).buildAsyncFuture(fetch(_.pri))

    private[UserApi] def fetch(
        readPref: ReadPref
    )(userIds: PairOf[Option[UserId]], perf: PerfKey): Fu[GameUsers] =
      val (x, y) = userIds
      listWithPerf(List(x, y).flatten, perf, readPref).map: users =>
        ByColor(x, y).map(_.flatMap(id => users.find(_.id == id)))

  def updatePerfs(ups: ByColor[(UserPerfs, UserPerfs)], gamePerfType: PerfType): Funit =
    import lila.memo.CacheApi.invalidate
    for _ <- ups.all.map(perfsRepo.updatePerfs).parallelVoid
    yield gamePlayers.cache.invalidate(ups.map(_._1.id.some).toPair -> gamePerfType)

  def withPerfs[U: UserIdOf](u: U): Fu[Option[UserWithPerfs]] = u.id
    .isnt(UserId.undefined)
    .so:
      userRepo.coll
        .aggregateOne(): framework =>
          import framework.*
          Match($id(u.id)) -> List:
            PipelineOperator(perfsRepo.aggregate.lookup)
        .map: docO =>
          for
            doc <- docO
            user <- doc.asOpt[User]
            perfs = perfsRepo.aggregate.readFirst(doc, user)
          yield UserWithPerfs(user, perfs)

  def enabledWithPerf[U: UserIdOf](id: U, perfType: PerfType): Fu[Option[WithPerf]] =
    byIdWithPerf(id, perfType).dmap(_.filter(_.user.enabled.yes))

  def listWithPerfs[U: UserIdOf](us: List[U], includeClosed: Boolean): Fu[List[UserWithPerfs]] =
    us.nonEmpty.so:
      val ids = us.map(_.id)
      userRepo.coll
        .aggregateList(Int.MaxValue): framework =>
          import framework.*
          Match($inIds(ids) ++ includeClosed.not.so(userRepo.enabledSelect)) -> List(
            PipelineOperator(perfsRepo.aggregate.lookup),
            AddFields($sort.orderField(ids)),
            Sort(Ascending("_order"))
          )
        .map: docs =>
          for
            doc <- docs
            user <- doc.asOpt[User]
            perfs = perfsRepo.aggregate.readFirst(doc, user)
          yield UserWithPerfs(user, perfs)

  def byIdWithPerf[U: UserIdOf](id: U, pk: PerfKey): Fu[Option[WithPerf]] =
    userRepo.byId(id).flatMapz(perfsRepo.withPerf(_, pk).dmap(some))

  def pairWithPerfs(userIds: ByColor[Option[UserId]]): Fu[ByColor[Option[UserWithPerfs]]] =
    listWithPerfs(userIds.flatten, includeClosed = true).map: users =>
      userIds.map(_.flatMap(id => users.find(_.id == id)))

  def listWithPerf[U: UserIdOf](
      us: List[U],
      pk: PerfKey,
      readPref: ReadPref = _.sec
  ): Fu[List[WithPerf]] = us.nonEmpty.so:
    val ids = us.map(_.id)
    userRepo.coll
      .aggregateList(Int.MaxValue, readPref): framework =>
        import framework.*
        Match($inIds(ids)) -> List(
          PipelineOperator(perfsRepo.aggregate.lookup(pk)),
          AddFields($sort.orderField(ids)),
          Sort(Ascending("_order"))
        )
      .map: docs =>
        for
          doc <- docs
          user <- doc.asOpt[User]
          perf = perfsRepo.aggregate.readFirst(doc, pk)
        yield WithPerf(user, perf)

  def pairWithPerf(userIds: ByColor[Option[UserId]], pt: PerfType): Fu[ByColor[Option[WithPerf]]] =
    listWithPerf(userIds.flatten, pt).map: users =>
      userIds.map(_.flatMap(id => users.find(_.id == id)))

  def byIdOrGhostWithPerf(id: UserId, pt: PerfType): Fu[Option[LightUser.Ghost | WithPerf]] =
    userRepo
      .byIdOrGhost(id)
      .flatMapz:
        case Left(g) => fuccess(g.some)
        case Right(u) => perfsRepo.perfOf(u.id, pt).dmap(p => u.withPerf(p).some)

  def withIntRatingIn(userId: UserId, perf: PerfKey): Fu[Option[(User, IntRating)]] =
    byId(userId).flatMapz: user =>
      perfsRepo.intRatingOf(user.id, perf).map(r => (user, r).some)

  private def readEmails(doc: Bdoc) = lila.core.user.Emails(
    current = userRepo.anyEmail(doc),
    previous = doc.getAsOpt[NormalizedEmailAddress](BSONFields.prevEmail)
  )

  def withEmails[U: UserIdOf](users: List[U]): Fu[List[WithEmails]] =
    userRepo.coll
      .list[Bdoc]($inIds(users.map(_.id)), _.sec)
      .map: docs =>
        for
          doc <- docs
          user <- summon[BSONHandler[User]].readOpt(doc)
        yield WithEmails(user, readEmails(doc))

  def withEmails[U: UserIdOf](user: U): Fu[Option[WithEmails]] =
    withEmails(List(user)).dmap(_.headOption)

  def withPerfsAndEmails[U: UserIdOf](users: List[U]): Fu[List[WithPerfsAndEmails]] = for
    perfs <- perfsRepo.idsMap(users, _.sec)
    users <- userRepo.coll
      .list[Bdoc]($inIds(users.map(_.id)), _.sec)
      .map: docs =>
        for
          doc <- docs
          user <- summon[BSONReader[User]].readOpt(doc)
        yield WithPerfsAndEmails(lila.rating.UserWithPerfs(user, perfs.get(user.id)), readEmails(doc))
  yield users

  def withPerfsAndEmails[U: UserIdOf](u: U): Fu[Option[WithPerfsAndEmails]] =
    withPerfsAndEmails(List(u)).map(_.headOption)

  def setBot(user: User): Funit =
    if user.count.game > 0
    then fufail(LilaInvalid("You already have games played. Make a new account."))
    else
      userRepo.setTitle(user.id, PlayerTitle.BOT) >>
        userRepo.setRoles(user.id, Nil) >>
        perfsRepo.setBotInitialPerfs(user.id)

  def setKid(user: User, v: KidMode): Fu[Option[KidMode]] =
    (user.kid != v)
      .option:
        for
          _ <- userRepo.setKid(user, v)
          _ = lila.common.Bus.pub(SetKidMode(user.copy(kid = v)))
        yield v
      .sequence

  def visibleBotsByIds(ids: Iterable[UserId]): Fu[List[UserWithPerfs]] =
    userRepo.coll
      .aggregateList(onlineBotVisible.value, _.sec): framework =>
        import framework.*
        Match($inIds(ids) ++ userRepo.botWithBioSelect ++ userRepo.enabledSelect ++ userRepo.notLame) -> List(
          Sort(Descending(BSONFields.roles), Descending("time.human")),
          Limit(onlineBotVisible.value),
          PipelineOperator(perfsRepo.aggregate.lookup)
        )
      .map: docs =>
        for
          doc <- docs
          user <- doc.asOpt[User]
          perfs = perfsRepo.aggregate.readFirst(doc, user)
        yield UserWithPerfs(user, perfs)

  def byIdsSortRatingNoBot(ids: Iterable[UserId], nb: Int): Fu[List[UserWithPerfs]] =
    perfsRepo.coll
      .aggregateList(nb, _.sec): framework =>
        import framework.*
        import lila.user.BSONFields as F
        Match(
          $inIds(ids) ++ $doc("standard.gl.d".$lt(chess.rating.glicko.provisionalDeviation))
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
              s"user.${F.marks}".$nin(List(UserMark.engine, UserMark.boost)),
              s"user.${F.title}".$ne(PlayerTitle.BOT)
            )
          ,
          Limit(nb)
        )
      .map: docs =>
        for
          doc <- docs
          user <- doc.getAsOpt[User]("user")
          perfs = perfsRepo.aggregate.readFrom(doc, user)
        yield UserWithPerfs(user, perfs)
