package lila.swiss

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.*
import reactivemongo.api.bson.*

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.format.{ DateTimeFormatter, FormatStyle }

import lila.common.Bus
import lila.core.LightUser
import lila.core.round.RoundBus
import lila.core.swiss.{ IdName, SwissFinish }
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }
import lila.gathering.Condition.WithVerdicts
import lila.gathering.GreatPlayer

final class SwissApi(
    mongo: SwissMongo,
    cache: SwissCache,
    socket: SwissSocket,
    director: SwissDirector,
    scoring: SwissScoring,
    rankingApi: SwissRankingApi,
    standingApi: SwissStandingApi,
    banApi: SwissBanApi,
    boardApi: SwissBoardApi,
    verify: SwissCondition.Verify,
    chatApi: lila.core.chat.ChatApi,
    userApi: lila.core.user.UserApi,
    lightUserApi: lila.core.user.LightUserApi,
    roundApi: lila.core.round.RoundApi
)(using scheduler: Scheduler)(using Executor, akka.stream.Materializer, lila.core.config.RateLimit)
    extends lila.core.swiss.SwissApi:

  private val sequencer = scalalib.actor.AsyncActorSequencers[SwissId](
    maxSize = Max(1024), // queue many game finished events
    expiration = 20.minutes,
    timeout = 10.seconds,
    name = "swiss.api",
    lila.log.asyncActorMonitor.full
  )

  import BsonHandlers.{ *, given }

  def fetchByIdNoCache(id: SwissId) = mongo.swiss.byId[Swiss](id)

  def create(data: SwissForm.SwissData, teamId: TeamId)(using me: Me): Fu[Swiss] =
    val swiss = Swiss(
      id = Swiss.makeId,
      name = data.name | GreatPlayer.randomName,
      clock = data.clock,
      variant = data.realVariant,
      round = SwissRoundNumber(0),
      nbPlayers = 0,
      nbOngoing = 0,
      createdAt = nowInstant,
      createdBy = me,
      teamId = teamId,
      nextRoundAt = data.realStartsAt.some,
      startsAt = data.realStartsAt,
      finishedAt = none,
      winnerId = none,
      settings = Swiss.Settings(
        nbRounds = data.nbRounds,
        rated = chess.Rated(data.isRated && data.realPosition.isEmpty),
        description = data.description,
        position = data.realPosition,
        chatFor = data.realChatFor,
        roundInterval = data.realRoundInterval,
        password = data.password,
        conditions = data.conditions,
        forbiddenPairings = ~data.forbiddenPairings,
        manualPairings = ~data.manualPairings
      )
    )
    for
      _ <- mongo.swiss.insert.one(addFeaturable(swiss))
      _ = cache.featuredInTeam.invalidate(swiss.teamId)
    yield swiss

  def update(swissId: SwissId, data: SwissForm.SwissData): Fu[Option[Swiss]] =
    Sequencing(swissId)(cache.swissCache.byId): old =>
      val position =
        if old.isCreated || old.settings.position.isDefined then
          data.realVariant.standard.so(data.realPosition)
        else old.settings.position
      val swiss = old
        .copy(
          name = data.name | old.name,
          clock = if old.isCreated then data.clock else old.clock,
          variant = if old.isCreated && data.variant.isDefined then data.realVariant else old.variant,
          startsAt = data.startsAt.ifTrue(old.isCreated) | old.startsAt,
          nextRoundAt =
            if old.isCreated then Some(data.startsAt | old.startsAt)
            else old.nextRoundAt,
          settings = old.settings.copy(
            nbRounds = data.nbRounds,
            rated = data.rated.getOrElse(old.settings.rated).map(_ && position.isEmpty),
            description = data.description.orElse(old.settings.description),
            position = position,
            chatFor = data.chatFor | old.settings.chatFor,
            roundInterval =
              if data.roundInterval.isDefined then data.realRoundInterval
              else old.settings.roundInterval,
            password = data.password,
            conditions = data.conditions,
            forbiddenPairings = ~data.forbiddenPairings,
            manualPairings = ~data.manualPairings
          )
        )
        .pipe: s =>
          if s.isStarted && s.nbOngoing == 0 && (s.nextRoundAt.isEmpty || old.settings.manualRounds) && !s.settings.manualRounds
          then s.copy(nextRoundAt = nowInstant.plusSeconds(s.settings.roundInterval.toSeconds.toInt).some)
          else if s.settings.manualRounds && !old.settings.manualRounds then s.copy(nextRoundAt = none)
          else s
      for
        _ <- mongo.swiss.update.one($id(old.id), addFeaturable(swiss))
        _ <- (swiss.perfType != old.perfType).so(recomputePlayerRatings(swiss))
      yield
        cache.swissCache.clear(swiss.id)
        cache.roundInfo.put(swiss.id, fuccess(swiss.roundInfo.some))
        socket.reload(swiss.id)
        swiss.some

  private def recomputePlayerRatings(swiss: Swiss): Funit = for
    ranking <- rankingApi(swiss)
    perfs <- userApi.perfOf(ranking.keys, swiss.perfType)
    update = mongo.player.update(ordered = false)
    elements <- perfs.parallel: (userId, perf) =>
      update.element(
        q = $id(SwissPlayer.makeId(swiss.id, userId)),
        u = $set(
          SwissPlayer.Fields.rating -> perf.intRating,
          SwissPlayer.Fields.provisional -> perf.provisional.yes.option(true)
        )
      )
    _ <- elements.nonEmpty.so(update.many(elements).void)
  yield ()

  def scheduleNextRound(swiss: Swiss, date: Instant): Funit =
    Sequencing(swiss.id)(cache.swissCache.notFinishedById): old =>
      for
        _ <- (!old.settings.manualRounds).so(
          mongo.swiss
            .updateField($id(old.id), "settings.i", Swiss.RoundInterval.manual)
            .void
        )
        _ <- old.isCreated.so(mongo.swiss.updateField($id(old.id), "startsAt", date).void)
        _ <- (!old.isFinished && old.nbOngoing == 0)
          .so(mongo.swiss.updateField($id(old.id), "nextRoundAt", date).void)
        formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        showDate = formatter.print(date)
      yield
        systemChat(swiss.id, s"Round ${swiss.round.value + 1} scheduled at $showDate UTC")
        cache.swissCache.clear(swiss.id)
        socket.reload(swiss.id)

  def verdicts(swiss: Swiss)(using me: Option[Me]): Fu[WithVerdicts] =
    me.foldUse(fuccess(swiss.settings.conditions.accepted)): me ?=>
      userApi
        .withPerf(me, swiss.perfType)
        .flatMap: user =>
          given Perf = user.perf
          verify(swiss)

  private val initialJoin =
    lila.memo.RateLimit.composite[UserId]("swiss.user.join")(("fast", 4, 1.hour), ("slow", 12, 1.day))

  def join(id: SwissId, isInTeam: TeamId => Boolean, password: Option[String])(using
      me: Me
  ): Fu[Option[String]] =
    Sequencing(id)(cache.swissCache.notFinishedById): swiss =>
      if me.marks.prizeban && swiss.looksLikePrize
      then fuccess("You are not allowed to play in prized tournaments".some)
      else if !isInTeam(swiss.teamId)
      then fuccess("You are not in the team of this tournament".some)
      else if !swiss.settings.password.forall: p =>
          MessageDigest.isEqual(p.getBytes(UTF_8), (~password).getBytes(UTF_8))
      then fuccess("Wrong entry code".some)
      else
        mongo.player // try a rejoin first
          .updateField($id(SwissPlayer.makeId(swiss.id, me)), SwissPlayer.Fields.absent, false)
          .flatMap: rejoin =>
            if rejoin.n == 1 then fuccess(none) // if the match failed (not the update!), try a join
            else if !initialJoin.test(me.userId) then fuccess("You are joining too many tournaments".some)
            else
              for
                user <- userApi.withPerf(me.value, swiss.perfType)
                given Perf = user.perf
                verified <- verify(swiss)
                error =
                  if !verified.accepted then "You don't satisfy the tournament requirements".some
                  else if !swiss.isEnterable then "This tournament cannot be entered".some
                  else none
                _ <- error.isEmpty.so:
                  mongo.player.insert
                    .one(SwissPlayer.make(swiss.id, user))
                    .zip(mongo.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> 1)))
                    .void
              yield
                cache.swissCache.clear(swiss.id)
                error
          .flatMap: res =>
            recomputeAndUpdateAll(id).inject(res)

  def gameIdSource(
      swissId: SwissId,
      player: Option[UserId],
      batchSize: Int = 0,
      readPref: ReadPref = _.sec
  ): Source[GameId, ?] =
    SwissPairing.fields: f =>
      mongo.pairing
        .find($doc(f.swissId -> swissId) ++ player.so(u => $doc(f.players -> u)), $id(true).some)
        .sort($sort.asc(f.round))
        .batchSize(batchSize)
        .cursor[Bdoc](readPref)
        .documentSource()
        .mapConcat(_.getAsOpt[GameId]("_id").toList)

  def featuredInTeam(teamId: TeamId): Fu[List[Swiss]] =
    cache.featuredInTeam.get(teamId).flatMap { ids =>
      mongo.swiss.byOrderedIds[Swiss, SwissId](ids)(_.id)
    }

  def visibleByTeam(teamId: TeamId, nbPast: Int, nbSoon: Int): Fu[Swiss.PastAndNext] =
    (nbPast > 0)
      .so:
        mongo.swiss
          .find($doc("teamId" -> teamId, "finishedAt".$exists(true)))
          .sort($sort.desc("startsAt"))
          .cursor[Swiss]()
          .list(nbPast)
      .zip((nbSoon > 0).so {
        mongo.swiss
          .find(
            $doc("teamId" -> teamId, "startsAt".$gt(nowInstant.minusWeeks(2)), "finishedAt".$exists(false))
          )
          .sort($sort.asc("startsAt"))
          .cursor[Swiss]()
          .list(nbSoon)
      })
      .map((Swiss.PastAndNext.apply).tupled)

  def playerInfo(swiss: Swiss, userId: UserId): Fu[Option[SwissPlayer.ViewExt]] =
    userApi.byId(userId).flatMapz { user =>
      mongo.player.byId[SwissPlayer](SwissPlayer.makeId(swiss.id, user.id).value).flatMapz { player =>
        SwissPairing
          .fields { f =>
            mongo.pairing
              .find($doc(f.swissId -> swiss.id, f.players -> player.userId))
              .sort($sort.asc(f.round))
              .cursor[SwissPairing]()
              .listAll()
          }
          .flatMap:
            pairingViews(_, player)
          .flatMap { pairings =>
            SwissPlayer
              .fields { f =>
                mongo.player.countSel($doc(f.swissId -> swiss.id, f.score.$gt(player.score))).dmap(1.+)
              }
              .map { rank =>
                val pairingMap = pairings.mapBy(_.pairing.round)
                SwissPlayer
                  .ViewExt(
                    player,
                    rank,
                    user.light,
                    pairingMap,
                    SwissSheet.one(swiss, pairingMap.view.mapValues(_.pairing).toMap, player)
                  )
                  .some
              }
          }

      }

    }

  def pairingViews(pairings: Seq[SwissPairing], player: SwissPlayer): Fu[Seq[SwissPairing.View]] =
    pairings.headOption.so: first =>
      mongo.player
        .list[SwissPlayer]($inIds(pairings.map(_.opponentOf(player.userId)).map {
          SwissPlayer.makeId(first.swissId, _)
        }))
        .flatMap: opponents =>
          lightUserApi
            .asyncMany(opponents.map(_.userId))
            .map { users =>
              opponents.zip(users).map { case (o, u) =>
                SwissPlayer.WithUser(o, u | LightUser.fallback(o.userId.into(UserName)))
              }
            }
            .map { opponents =>
              pairings.flatMap { pairing =>
                opponents
                  .find(_.player.userId == pairing.opponentOf(player.userId))
                  .map:
                    SwissPairing.View(pairing, _)
              }
            }

  def searchPlayers(id: SwissId, term: UserSearch, nb: Int): Fu[List[UserId]] =
    SwissPlayer.fields: f =>
      mongo.player.primitive[UserId](
        selector = $doc(
          f.swissId -> id,
          f.userId.$startsWith(term.value)
        ),
        sort = $sort.desc(f.score),
        nb = nb,
        field = f.userId
      )

  def pageOf(swiss: Swiss, userId: UserId): Fu[Option[Int]] =
    rankingApi(swiss).map:
      _.get(userId).map { rank =>
        (rank - 1).value / 10 + 1
      }

  def gameView(pov: Pov): Fu[Option[GameView]] =
    (pov.game.swissId.so(cache.swissCache.byId)).flatMapz { swiss =>
      getGameRanks(swiss, pov.game).dmap:
        GameView(swiss, _).some
    }

  private def getGameRanks(swiss: Swiss, game: Game): Fu[Option[GameRanks]] =
    swiss.isStarted.so:
      game.players
        .traverse(_.userId)
        .so: ids =>
          rankingApi(swiss).map: ranking =>
            ids.traverseReduce(ranking.get)(GameRanks.apply)

  private[swiss] def leaveTeam(teamId: TeamId, userId: UserId) =
    joinedPlayableSwissIds(userId, List(teamId))
      .flatMap { kickFromSwissIds(userId, _) }

  private[swiss] def kickLame(userId: UserId) =
    Bus
      .ask[List[TeamId], lila.core.team.TeamIdsJoinedBy](lila.core.team.TeamIdsJoinedBy(userId, _))
      .flatMap(joinedPlayableSwissIds(userId, _))
      .flatMap(kickFromSwissIds(userId, _, forfeit = true))

  def joinedPlayableSwissIds(userId: UserId, teamIds: List[TeamId]): Fu[List[SwissId]] =
    mongo.swiss
      .aggregateList(100, _.sec): framework =>
        import framework.*
        Match($doc("teamId".$in(teamIds), "featurable" -> true)) -> List(
          PipelineOperator(
            $lookup.pipeline(
              as = "player",
              from = mongo.player.name,
              local = "_id",
              foreign = "s",
              pipe = List($doc("$match" -> $doc("u" -> userId)))
            )
          ),
          Match("player".$ne($arr())),
          Limit(100),
          Project($id(true))
        )
      .map(_.flatMap(_.getAsOpt[SwissId]("_id")))

  private def kickFromSwissIds(userId: UserId, swissIds: List[SwissId], forfeit: Boolean = false): Funit =
    swissIds.sequentiallyVoid(withdraw(_, userId, forfeit))

  def withdraw(id: SwissId, userId: UserId, forfeit: Boolean = false): Funit =
    Sequencing(id)(cache.swissCache.notFinishedById): swiss =>
      SwissPlayer.fields: f =>
        val selId = $id(SwissPlayer.makeId(swiss.id, userId))
        if swiss.isStarted then
          mongo.player.updateField(selId, f.absent, true) >>
            forfeit.so { forfeitPairings(swiss, userId) }
        else
          mongo.player.delete.one(selId).flatMap { res =>
            (res.n == 1).so:
              for _ <- mongo.swiss.update.one($id(swiss.id), $inc("nbPlayers" -> -1))
              yield cache.swissCache.clear(swiss.id)
          }
    .void >> recomputeAndUpdateAll(id)

  private def forfeitPairings(swiss: Swiss, userId: UserId): Funit =
    SwissPairing.fields: F =>
      mongo.pairing
        .list[SwissPairing]($doc(F.swissId -> swiss.id, F.players -> userId))
        .flatMap:
          _.filter(p => p.isDraw || userId.is(p.winner))
            .parallelVoid: pairing =>
              mongo.pairing.update.one($id(pairing.id), pairing.forfeit(userId))

  private[swiss] def finishGame(game: Game): Funit =
    game.swissId.so: swissId =>
      Sequencing(swissId)(cache.swissCache.byId): swiss =>
        if !swiss.isStarted then
          logger.info(s"Removing pairing ${game.id} finished after swiss ${swiss.id}")
          mongo.pairing.delete.one($id(game.id)).inject(false)
        else
          for
            result <- mongo.pairing.updateField(
              $id(game.id),
              SwissPairing.Fields.status,
              Right(game.winnerColor): SwissPairing.Status
            )
            isModified <-
              if result.nModified == 0
              then fuccess(false) // dedup
              else
                for
                  _ <-
                    if swiss.nbOngoing > 0 then mongo.swiss.update.one($id(swiss.id), $inc("nbOngoing" -> -1))
                    else
                      fuccess:
                        logger.warn(s"swiss ${swiss.id} nbOngoing = ${swiss.nbOngoing}")
                  _ <- game.playerWhoDidNotMove.flatMap(_.userId).so { absent =>
                    SwissPlayer.fields: f =>
                      mongo.player
                        .updateField($doc(f.swissId -> swiss.id, f.userId -> absent), f.absent, true)
                        .void
                  }
                  _ <- (swiss.nbOngoing <= 1).so(onRoundFinish(swiss, game.some))
                yield true
            _ = cache.swissCache.clear(swiss.id)
          yield isModified
      .flatMapz:
        recomputeAndUpdateAll(swissId) >> banApi.onGameFinish(game)

  private def onRoundFinish(swiss: Swiss, lastGame: Option[Game]): Funit =
    if swiss.round.value == swiss.settings.nbRounds then doFinish(swiss)
    else if swiss.settings.manualRounds then
      fuccess:
        systemChat(swiss.id, s"Round ${swiss.round.value + 1} needs to be scheduled.")
    else
      for
        nextRoundAt <- swiss.settings.dailyInterval match
          case Some(days) =>
            lastGame match
              case Some(g) => fuccess(g.createdAt.plusDays(days))
              case None => lastRoundAt(swiss).map(_ | nowInstant)
          case None => fuccess(nowInstant.plusSeconds(swiss.settings.roundInterval.toSeconds.toInt))
        _ <- mongo.swiss.updateField($id(swiss.id), "nextRoundAt", nextRoundAt)
      yield systemChat(swiss.id, s"Round ${swiss.round.value + 1} will start soon.")

  private def lastRoundAt(swiss: Swiss): Fu[Option[Instant]] =
    mongo.swiss.primitiveOne[Instant]($id(swiss.id), "lastRoundAt")

  private[swiss] def destroy(swiss: Swiss): Funit = for
    _ <- mongo.swiss.delete.one($id(swiss.id))
    _ <- mongo.pairing.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id))
    _ <- mongo.player.delete.one($doc(SwissPairing.Fields.swissId -> swiss.id))
  yield
    cache.swissCache.clear(swiss.id)
    socket.reload(swiss.id)

  private[swiss] def finish(oldSwiss: Swiss): Funit =
    Sequencing(oldSwiss.id)(cache.swissCache.startedById): swiss =>
      mongo.pairing
        .exists($doc(SwissPairing.Fields.swissId -> swiss.id))
        .flatMap:
          if _ then doFinish(swiss)
          else destroy(swiss)
  private def doFinish(swiss: Swiss): Funit = for
    winnerUserId <- SwissPlayer.fields: f =>
      mongo.player.primitiveOne[UserId]($doc(f.swissId -> swiss.id), $sort.desc(f.score), f.userId)
    _ <- mongo.swiss.update
      .one(
        $id(swiss.id),
        $unset("nextRoundAt", "lastRoundAt", "featurable") ++ $set(
          "settings.n" -> swiss.round,
          "finishedAt" -> nowInstant,
          "winnerId" -> winnerUserId
        )
      )
    pairingDelete <- SwissPairing.fields: f =>
      mongo.pairing.delete.one($doc(f.swissId -> swiss.id, f.status -> true))
  yield
    if pairingDelete.n > 0
    then logger.warn(s"Swiss ${swiss.id} finished with ${pairingDelete.n} ongoing pairings")
    systemChat(swiss.id, s"Tournament completed!")
    cache.swissCache.clear(swiss.id)
    socket.reload(swiss.id)
    scheduler
      .scheduleOnce(10.seconds):
        // we're delaying this to make sure the ranking has been recomputed
        // since doFinish is called by finishGame before that
        rankingApi(swiss).foreach: ranking =>
          Bus.pub(SwissFinish(swiss.id, ranking))

  def kill(swiss: Swiss): Funit = for _ <-
      if swiss.isStarted then
        for _ <- finish(swiss)
        yield
          logger.info(s"Tournament ${swiss.id} cancelled by its creator.")
          systemChat(swiss.id, "Tournament cancelled by its creator.")
      else if swiss.isCreated then destroy(swiss)
      else funit
  yield cache.featuredInTeam.invalidate(swiss.teamId)

  def roundInfo = cache.roundInfo.get

  def byTeamCursor(
      teamId: TeamId,
      status: Option[Swiss.Status],
      createdBy: Option[UserStr],
      name: Option[String]
  ) =
    val statusSel = status.so:
      case Swiss.Status.created => $doc("round" -> 0)
      case Swiss.Status.started => $doc("round".$gt(0), "finishedAt" -> $exists(false))
      case Swiss.Status.finished => $doc("finishedAt" -> $exists(true))
    val creatorSel = createdBy.so(u => $doc("createdBy" -> u))
    val nameSel = name.so(n => $doc("name" -> n))
    mongo.swiss
      .find:
        $doc("teamId" -> teamId) ++ statusSel ++ creatorSel ++ nameSel
      .sort($sort.desc("startsAt"))
      .cursor[Swiss]()

  def teamOf(id: SwissId): Fu[Option[TeamId]] =
    mongo.swiss.primitiveOne[TeamId]($id(id), "teamId")

  def maybeRecompute(swiss: Swiss): Unit =
    if swiss.isStarted && periodicRecompute(swiss.id)
    then
      Sequencing(swiss.id)(cache.swissCache.notFinishedById): s =>
        recomputeAndUpdateAll(s.id)

  private val periodicRecompute = scalalib.cache.OnceEvery[SwissId](10.minutes)

  private def recomputeAndUpdateAll(id: SwissId): Funit =
    scoring
      .compute(id)
      .flatMapz: res =>
        rankingApi.update(res)
        for
          _ <- standingApi.update(res)
          _ <- boardApi.update(res)
          ongoingPairings = res.countOngoingPairings
          _ <- (ongoingPairings != res.swiss.nbOngoing).so:
            logger.warn:
              s"Swiss(${id}).nbOngoing = ${res.swiss.nbOngoing}, but res.countOngoingPairings = ${ongoingPairings}"
            for
              _ <- mongo.swiss.updateField($id(id), "nbOngoing", ongoingPairings)
              _ <- (ongoingPairings == 0).so(onRoundFinish(res.swiss, none))
            yield ()
        yield socket.reload(id)

  private[swiss] def startPendingRounds: Funit =
    mongo.swiss
      .find($doc("nextRoundAt".$lt(nowInstant)), $id(true).some)
      .cursor[Bdoc]()
      .list(10)
      .map(_.flatMap(_.getAsOpt[SwissId]("_id")))
      .flatMap:
        _.sequentiallyVoid: id =>
          Sequencing(id)(cache.swissCache.notFinishedById) { swiss =>
            if swiss.round.value >= swiss.settings.nbRounds then doFinish(swiss)
            else if swiss.nbPlayers >= 2 then
              countPresentPlayers(swiss).flatMap { nbPresent =>
                if nbPresent < 2 then
                  systemChat(swiss.id, "Not enough players left.")
                  doFinish(swiss)
                else
                  for
                    next <- director.startRound(swiss)
                    _ <- next match
                      case None =>
                        systemChat(swiss.id, "All possible pairings were played.")
                        doFinish(swiss)
                      case Some(s) if s.nextRoundAt.isEmpty =>
                        systemChat(s.id, s"Round ${s.round.value} started.")
                        funit
                      case Some(s) =>
                        systemChat(s.id, s"Round ${s.round.value} failed.", volatile = true)
                        mongo.swiss.update
                          .one($id(s.id), $set("nextRoundAt" -> nowInstant.plusSeconds(61)))
                          .void
                  yield cache.swissCache.clear(swiss.id)
              }
            else if swiss.startsAt.isBefore(nowInstant.minusMinutes(60)) then destroy(swiss)
            else
              systemChat(swiss.id, "Not enough players for first round; delaying start.", volatile = true)
              for _ <- mongo.swiss.update.one(
                  $id(swiss.id),
                  $set("nextRoundAt" -> nowInstant.plusSeconds(121))
                )
              yield cache.swissCache.clear(swiss.id)
          } >> recomputeAndUpdateAll(id)
      .monSuccess(_.swiss.tick)

  private def countPresentPlayers(swiss: Swiss) = SwissPlayer.fields: f =>
    mongo.player.countSel($doc(f.swissId -> swiss.id, f.absent.$ne(true)))

  private[swiss] def checkOngoingGames: Funit =
    SwissPairing
      .fields: f =>
        mongo.pairing
          .aggregateList(100): framework =>
            import framework.*
            Match($doc(f.status -> SwissPairing.ongoing)) -> List(
              GroupField(f.swissId)("ids" -> PushField(f.id)),
              Limit(100)
            )
      .map:
        _.flatMap: doc =>
          for
            swissId <- doc.getAsOpt[SwissId]("_id")
            gameIds <- doc.getAsOpt[List[GameId]]("ids")
          yield swissId -> gameIds
      .flatMap:
        _.sequentiallyVoid { (swissId, gameIds) =>
          Sequencing[List[Game]](swissId)(cache.swissCache.byId) { _ =>
            roundApi
              .getGames(gameIds)
              .map: pairs =>
                val games = pairs.collect { case (_, Some(g)) => g }
                val (finished, ongoing) = games.partition(_.finishedOrAborted)
                val flagged = ongoing.filter(_.outoftime(true))
                val missingIds = pairs.collect { case (id, None) => id }
                lila.mon.swiss.games("finished").record(finished.size)
                lila.mon.swiss.games("ongoing").record(ongoing.size)
                lila.mon.swiss.games("flagged").record(flagged.size)
                lila.mon.swiss.games("missing").record(missingIds.size)
                if flagged.nonEmpty then
                  Bus.pub(
                    lila.core.round.TellMany(flagged.map(_.id), RoundBus.QuietFlag)
                  )
                if missingIds.nonEmpty then mongo.pairing.delete.one($inIds(missingIds))
                finished
          }.flatMap:
            _.sequentiallyVoid(finishGame)
        }

  private def systemChat(id: SwissId, text: String, volatile: Boolean = false): Unit =
    if volatile
    then chatApi.volatile(id.into(ChatId), text, _.swiss)
    else chatApi.system(id.into(ChatId), text, _.swiss)

  def withdrawAll(user: User, teamIds: List[TeamId]): Funit =
    mongo.swiss
      .aggregateList(Int.MaxValue, _.sec): framework =>
        import framework.*
        Match($doc("finishedAt".$exists(false), "nbPlayers".$gt(0), "teamId".$in(teamIds))) -> List(
          PipelineOperator(
            $lookup.pipelineFull(
              from = mongo.player.name,
              let = $doc("s" -> "$_id"),
              as = "player",
              pipe = List(
                $doc(
                  "$match" -> $expr(
                    $and(
                      $doc("$eq" -> $arr("$u", user.id)),
                      $doc("$eq" -> $arr("$s", "$$s"))
                    )
                  )
                )
              )
            )
          ),
          Match("player".$ne($arr())),
          Project($id(true))
        )
      .map(_.flatMap(_.getAsOpt[SwissId]("_id")))
      .flatMap:
        _.sequentiallyVoid { withdraw(_, user.id) }

  def isUnfinished(id: SwissId): Fu[Boolean] =
    mongo.swiss.exists($id(id) ++ $doc("finishedAt".$exists(false)))

  def filterPlaying(id: SwissId, userIds: Seq[UserId]): Fu[List[UserId]] =
    userIds.nonEmpty
      .so(mongo.swiss.exists($id(id) ++ $doc("finishedAt".$exists(false))))
      .flatMapz:
        SwissPlayer.fields: f =>
          mongo.player.distinctEasy[UserId, List](
            f.userId,
            $doc(
              f.id.$in(userIds.map(SwissPlayer.makeId(id, _))),
              f.absent.$ne(true)
            )
          )

  def resultStream(swiss: Swiss, perSecond: MaxPerSecond, nb: Int): Source[SwissPlayer.WithRank, ?] =
    SwissPlayer.fields: f =>
      mongo.player
        .find($doc(f.swissId -> swiss.id))
        .sort($sort.desc(f.score))
        .batchSize(perSecond.value)
        .cursor[SwissPlayer](ReadPref.sec)
        .documentSource(nb)
        .throttle(perSecond.value, 1.second)
        .zipWithIndex
        .map: (player, index) =>
          SwissPlayer.WithRank(player, index.toInt + 1)

  private val idNameProjection = $doc("name" -> true)

  def idNames(ids: List[SwissId]): Fu[List[IdName]] =
    mongo.swiss.find($inIds(ids), idNameProjection.some).cursor[IdName]().listAll()

  def onUserDelete(u: UserId, ids: Set[SwissId]) = for
    _ <- mongo.swiss.update.one(
      $inIds(ids) ++ $doc("winnerId" -> u),
      $set("winnerId" -> UserId.ghost),
      multi = true
    )
    playerIds = ids.map(SwissPlayer.makeId(_, u))
    players <- mongo.player.list[SwissPlayer]($inIds(playerIds), _.sec)
    // here we use a single ghost ID for all swiss players and pairings,
    // because the mapping of swiss player to swiss pairings must be preserved
    ghostId = UserId(s"!${scalalib.ThreadLocalRandom.nextString(8)}")
    newPlayers = players.map: p =>
      p.copy(id = SwissPlayer.makeId(p.swissId, ghostId), userId = ghostId)
    _ <- mongo.player.delete.one($inIds(playerIds))
    _ <- mongo.player.insert.many(newPlayers)
    _ <- mongo.pairing.update.one($doc("s".$in(ids), "p" -> u), $set("p.$" -> ghostId), multi = true)
  yield ()

  private def Sequencing[A <: Matchable: alleycats.Zero](
      id: SwissId
  )(fetch: SwissId => Fu[Option[Swiss]])(run: Swiss => Fu[A]): Fu[A] =
    sequencer(id):
      fetch(id).flatMapz(run)
