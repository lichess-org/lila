package lila.tournament

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import akka.stream.scaladsl.*
import com.roundeights.hasher.Algo
import play.api.libs.json.*
import scalalib.paginator.Paginator
import scalalib.Debouncer
import chess.{ IntRating, ByColor }
import alleycats.Zero

import lila.common.Bus
import lila.core.game.LightPov
import lila.core.round.{ RoundBus, GoBerserk }
import lila.core.team.LightTeam
import lila.core.tournament.Status
import lila.core.chess.Rank
import lila.gathering.Condition
import lila.gathering.Condition.GetMyTeamIds

final class TournamentApi(
    cached: TournamentCache,
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    apiJsonView: ApiJsonView,
    autoPairing: AutoPairing,
    pairingSystem: arena.PairingSystem,
    callbacks: TournamentApi.Callbacks,
    socket: TournamentSocket,
    leaderboard: LeaderboardApi,
    roundApi: lila.core.round.RoundApi,
    gameProxy: lila.core.game.GameProxy,
    trophyApi: lila.core.user.TrophyApi,
    colorHistoryApi: ColorHistoryApi,
    verify: TournamentCondition.Verify,
    duelStore: DuelStore,
    pause: Pause,
    waitingUsers: WaitingUsersApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.core.user.LightUserApi
)(using scheduler: Scheduler)(using
    Executor,
    akka.actor.ActorSystem,
    akka.stream.Materializer,
    lila.core.i18n.Translator,
    lila.core.config.RateLimit
):

  export tournamentRepo.byId as get

  def createTournament(
      setup: TournamentSetup,
      leaderTeams: List[LightTeam],
      andJoin: Boolean = true
  )(using me: Me): Fu[Tournament] =
    val tour = Tournament.fromSetup(setup)
    for
      _ <- tournamentRepo.insert(tour)
      _ <- setup.teamBattleByTeam
        .orElse(tour.conditions.teamMember.map(_.teamId))
        .so: teamId =>
          tournamentRepo.setForTeam(tour.id, teamId).void
      _ <- (andJoin && !me.isBot && !me.lame).so:
        val req = TournamentForm.TournamentJoin(setup.teamBattleByTeam, tour.password)
        join(tour.id, req, asLeader = false)(using _ => fuccess(leaderTeams.map(_.id)))
    yield tour

  def update(old: Tournament, data: TournamentSetup): Fu[Tournament] =
    updateTour(old, data, data.updateAll(old))

  def apiUpdate(old: Tournament, data: TournamentSetup): Fu[Tournament] =
    updateTour(old, data, data.updatePresent(old))

  private[tournament] def updateTour(
      old: Tournament,
      data: TournamentSetup,
      tour: Tournament
  ): Fu[Tournament] =
    val finalized = tour.copy(
      conditions = data.conditions
        .copy(teamMember = old.conditions.teamMember), // can't change that
      startsAt = if old.isCreated then tour.startsAt else old.startsAt
    )
    for
      _ <- tournamentRepo.update(finalized)
      _ <- ejectPlayersNonLongerOnAllowList(old, finalized)
      _ <- ejectBotPlayersNonLongerAllowed(old, finalized)
      _ = cached.tourCache.clear(tour.id)
    yield finalized

  private def ejectPlayersNonLongerOnAllowList(old: Tournament, tour: Tournament): Funit =
    tour.isCreated.so:
      tour.conditions
        .removedFromAllowList(old.conditions)
        .toList
        .sequentiallyVoid:
          withdraw(tour.id, _, false, false)

  private def ejectBotPlayersNonLongerAllowed(old: Tournament, tour: Tournament): Funit =
    (tour.isCreated && old.conditions.allowsBots && !tour.conditions.allowsBots).so:
      for
        botIds <- playerRepo.activeBotIds(tour.id)
        _ <- botIds.toList.sequentiallyVoid(withdraw(tour.id, _, false, false))
      yield ()

  def teamBattleUpdate(
      tour: Tournament,
      data: TeamBattle.DataForm.Setup,
      filterExistingTeamIds: Set[TeamId] => Fu[Set[TeamId]]
  ): Funit = for
    formTeamIds <- filterExistingTeamIds(data.potentialTeamIds.filterNot(TeamBattle.blacklist.contains))
    teamIds <-
      if !tour.isCreated
      then playerRepo.teamsWithPlayers(tour.id).map(_ ++ formTeamIds).map(_.take(TeamBattle.maxTeams))
      else fuccess(formTeamIds)
    _ <- tournamentRepo.setTeamBattle(tour.id, TeamBattle(teamIds, data.nbLeaders))
    _ <- tour.isCreated.so { playerRepo.removeNotInTeams(tour.id, teamIds) >> updateNbPlayers(tour.id) }
  yield
    cached.tourCache.clear(tour.id)
    socket.reload(tour.id)

  def teamBattleTeamInfo(tour: Tournament, teamId: TeamId): Fu[Option[TeamBattle.TeamInfo]] =
    tour.teamBattle.exists(_.teams(teamId)).optionFu(cached.teamInfo.get(tour.id -> teamId))

  private val hadPairings = scalalib.cache.ExpireSetMemo[TourId](1.hour)

  private[tournament] def makePairings(
      forTour: Tournament,
      users: WaitingUsers,
      smallTourNbActivePlayers: Option[Int]
  ): Funit =
    (users.size > 1 && (
      !hadPairings.get(forTour.id) ||
        users.haveWaitedEnough ||
        smallTourNbActivePlayers.exists(_ <= users.size * 1.5)
    )).so(Parallel(forTour.id, "makePairings")(cached.tourCache.started): tour =>
      cached
        .ranking(tour)
        .mon(_.tournament.pairing.createRanking)
        .flatMap: ranking =>
          pairingSystem
            .createPairings(tour, users, ranking, smallTourNbActivePlayers)
            .mon(_.tournament.pairing.createPairings)
            .flatMap:
              case Nil => funit
              case pairings =>
                pairingRepo.insert(pairings.map(_.pairing)) >>
                  pairings
                    .parallelVoid: pairing =>
                      autoPairing(tour, pairing, ranking.ranking)
                        .mon(_.tournament.pairing.createAutoPairing)
                        .map { socket.startGame(tour.id, _) }
                    .mon(_.tournament.pairing.createInserts)
                    .andDo:
                      lila.mon.tournament.pairing.batchSize.record(pairings.size)
                      waitingUsers.registerPairedUsers(
                        tour.id,
                        pairings.view.flatMap(_.pairing.users).toSet
                      )
                      socket.reload(tour.id)
                      hadPairings.put(tour.id)
                      featureOneOf(tour, pairings, ranking.ranking) // do outside of queue
        .monSuccess(_.tournament.pairing.create)
        .chronometer
        .logIfSlow(100, logger)(_ => s"Pairings for https://lichess.org/tournament/${tour.id}")
        .result)

  private def featureOneOf(tour: Tournament, pairings: List[Pairing.WithPlayers], ranking: Ranking): Funit =
    tour.featuredId
      .ifTrue(pairings.nonEmpty)
      .so(pairingRepo.byId)
      .map2(RankedPairing(ranking))
      .map(_.flatten)
      .flatMap { curOption =>
        pairings
          .flatMap(p => RankedPairing(ranking)(p.pairing))
          .minimumByOption(_.bestRank.value)
          .so: bestCandidate =>
            def switch = tournamentRepo.setFeaturedGameId(tour.id, bestCandidate.pairing.gameId)
            curOption.filter(_.pairing.playing) match
              case Some(current) if bestCandidate.bestRank < current.bestRank => switch
              case Some(_) => funit
              case _ => switch
      }

  private[tournament] def start(oldTour: Tournament): Funit =
    Parallel(oldTour.id, "start")(cached.tourCache.created): tour =>
      for _ <- tournamentRepo.setStatus(tour.id, Status.started)
      yield
        cached.tourCache.clear(tour.id)
        socket.reload(tour.id)
        publish()

  private[tournament] def destroy(tour: Tournament): Funit = for
    _ <- tournamentRepo.remove(tour).void
    _ <- pairingRepo.removeByTour(tour.id)
    _ <- playerRepo.removeByTour(tour.id)
  yield
    cached.tourCache.clear(tour.id)
    publish()
    socket.reload(tour.id)

  private[tournament] def finish(oldTour: Tournament): Funit =
    Parallel(oldTour.id, "finish")(cached.tourCache.started) { tour =>
      pairingRepo
        .count(tour.id)
        .flatMap:
          case 0 => destroy(tour)
          case _ =>
            for
              _ <- tournamentRepo.setStatus(tour.id, Status.finished)
              _ <- playerRepo.unWithdraw(tour.id)
              _ <- pairingRepo.removePlaying(tour.id)
              winner <- playerRepo.winner(tour.id)
              _ <- winner.so(p => tournamentRepo.setWinnerId(tour.id, p.userId))
            yield
              cached.tourCache.clear(tour.id)
              callbacks.clearJsonViewCache(tour)
              socket.finish(tour.id)
              publish()
              playerRepo
                .withPoints(tour.id)
                .foreach:
                  _.foreach: p =>
                    userApi.incToints(p.userId, p.score)
              awardTrophies(tour).logFailure(logger, _ => s"${tour.id} awardTrophies")
              callbacks.indexLeaderboard(tour).logFailure(logger, _ => s"${tour.id} indexLeaderboard")
              callbacks.clearWinnersCache(tour)
              callbacks.clearTrophyCache(tour)
              duelStore.remove(tour)
    }

  private[tournament] val killSchedule = scala.collection.mutable.Set.empty[TourId]

  def kill(tour: Tournament): Funit =
    if tour.isStarted then fuccess(killSchedule.add(tour.id)).void
    else if tour.isCreated then destroy(tour)
    else funit

  private def awardTrophies(tour: Tournament): Funit =
    import lila.core.user.TrophyKind.*
    import lila.tournament.Tournament.tournamentUrl
    tour.isMarathon.so:
      playerRepo
        .bestByTourWithRank(tour.id, 500)
        .flatMap:
          _.sequentiallyVoid:
            case rp if rp.rank.value == 1 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonWinner)
            case rp if rp.rank <= 10 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopTen)
            case rp if rp.rank <= 50 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopFifty)
            case rp if rp.rank <= 100 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopHundred)
            case rp => trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopFivehundred)

  def getVerdicts(tour: Tournament, playerExists: Boolean)(using
      GetMyTeamIds
  )(using
      me: Option[Me]
  ): Fu[Condition.WithVerdicts] =
    me.foldUse(fuccess(tour.conditions.accepted)): me ?=>
      if tour.isStarted && playerExists
      then verify.rejoin(tour.conditions)
      else
        userApi
          .usingPerfOf(me, tour.perfType):
            verify(tour.conditions, tour.perfType)

  private val initialJoin =
    lila.memo.RateLimit.composite[UserId]("tournament.user.join")(("fast", 8, 1.hour), ("slow", 30, 1.day))

  def join(
      tourId: TourId,
      data: TournamentForm.TournamentJoin,
      asLeader: Boolean
  )(using getMyTeamIds: GetMyTeamIds, me: Me): Fu[Tournament.JoinResult] =
    Parallel(tourId, "join")(cached.tourCache.enterable): tour =>
      import Tournament.JoinResult
      playerRepo
        .find(tour.id, me)
        .flatMap: prevPlayer =>
          if me.marks.arenaBan then fuccess(JoinResult.ArenaBanned)
          else if me.marks.prizeban && tour.prizeInDescription then fuccess(JoinResult.PrizeBanned)
          else if prevPlayer.isEmpty && !initialJoin.test(
              me.userId,
              cost = if asLeader then 0 else if tour.byLichess then 1 else 2
            )
          then fuccess(JoinResult.RateLimited)
          else if prevPlayer.nonEmpty || tour.password.forall: p =>
              // plain text access code
              MessageDigest.isEqual(p.getBytes(UTF_8), (~data.password).getBytes(UTF_8)) ||
                // user-specific access code: HMAC-SHA256(access code, user id)
                MessageDigest.isEqual(
                  Algo.hmac(p).sha256(me.userId.value).hex.getBytes(UTF_8),
                  (~data.password).getBytes(UTF_8)
                )
          then
            getVerdicts(tour, prevPlayer.isDefined).flatMap: verdicts =>
              if !verdicts.accepted then fuccess(JoinResult.Verdicts)
              else if !pause.canJoin(me, tour) then fuccess(JoinResult.Paused)
              else
                def proceedWithTeam(team: Option[TeamId]): Fu[JoinResult] = for
                  user <- userApi.withPerf(me.value, tour.perfType)
                  _ <- playerRepo.join(tour.id, user, team, prevPlayer)
                  _ <- updateNbPlayers(tour.id)
                yield
                  publish()
                  JoinResult.Ok
                tour.teamBattle.fold(proceedWithTeam(none)): battle =>
                  if prevPlayer.isDefined && tour.imminentStart then fuccess(JoinResult.Ok)
                  else
                    data.team match
                      case None if prevPlayer.isDefined => proceedWithTeam(none) // re-join ongoing
                      case Some(team) if battle.teams.contains(team) =>
                        getMyTeamIds(me.lightMe).flatMap: myTeams =>
                          if myTeams.has(team) then proceedWithTeam(team.some)
                          else fuccess(JoinResult.MissingTeam)
                      case _ => fuccess(JoinResult.MissingTeam)
          else fuccess(JoinResult.WrongEntryCode)
        .addEffect: result =>
          if result.ok then
            data.team
              .ifTrue(asLeader && tour.isTeamBattle)
              .foreach:
                tournamentRepo.setForTeam(tour.id, _)
            if ~data.pairMeAsap then
              pairingRepo.isPlaying(tourId, me).foreach { isPlaying =>
                if !isPlaying then waitingUsers.addApiUser(tour, me)
              }
          socket.reload(tour.id)
        .recover(lila.db.recoverDuplicateKey(_ => JoinResult.Ok))

  def pageOf(tour: Tournament, userId: UserId): Fu[Option[Int]] =
    cached
      .ranking(tour)
      .map:
        _.ranking
          .get(userId)
          .map:
            _.value / 10 + 1

  private object updateNbPlayers:
    private val onceEvery = scalalib.cache.OnceEvery[TourId](1.second)
    def apply(tourId: TourId): Funit = onceEvery(tourId).so:
      playerRepo.count(tourId).flatMap { tournamentRepo.setNbPlayers(tourId, _) }

  def selfPause(tourId: TourId, userId: UserId): Funit =
    withdraw(tourId, userId, isPause = true, isStalling = false)

  private[tournament] def withdraw(
      tourId: TourId,
      userId: UserId,
      isPause: Boolean,
      isStalling: Boolean
  ): Funit =
    Parallel(tourId, "withdraw")(cached.tourCache.enterable):
      case tour if tour.isCreated =>
        for
          _ <- playerRepo.remove(tour.id, userId)
          _ <- updateNbPlayers(tour.id)
        yield
          socket.reload(tour.id)
          publish()
      case tour if tour.isStarted =>
        for
          _ <- playerRepo.withdraw(tour.id, userId)
          pausable <-
            if isPause
            then cached.ranking(tour).map { _.ranking.get(userId).exists(_ < 7) }
            else fuccess(isStalling)
        yield
          if pausable then pause.add(userId)
          socket.reload(tour.id)
          publish()
      case _ => funit

  def withdrawAll(user: User): Funit =
    tournamentRepo
      .withdrawableIds(user.id, reason = "withdrawAll")
      .flatMap:
        _.sequentiallyVoid:
          withdraw(_, user.id, isPause = false, isStalling = false)

  private[tournament] def berserk(gameId: GameId, userId: UserId): Funit =
    gameProxy
      .game(gameId)
      .flatMap:
        _.filter(_.berserkable).so { game =>
          game.tournamentId.so: tourId =>
            lightUserApi
              .isBotSync(userId)
              .not
              .so:
                pairingRepo
                  .findPlaying(tourId, userId)
                  .flatMap:
                    case Some(pairing) if !pairing.berserkOf(userId) =>
                      pairing.colorOf(userId).so { color =>
                        roundApi
                          .ask(gameId)(GoBerserk(color, _))
                          .flatMapz:
                            pairingRepo.setBerserk(pairing, userId)
                      }
                    case _ => funit
        }

  private[tournament] def finishGame(game: Game): Funit =
    game.tournamentId.so: tourId =>
      pairingRepo
        .finishAndGet(game)
        .flatMap: pairingOpt =>
          Parallel(tourId, "finishGame")(cached.tourCache.started): tour =>
            for _ <- pairingOpt.so: pairing =>
                game.userIds.parallelVoid(updatePlayerAfterGame(tour, game, pairing))
            yield
              duelStore.remove(game)
              socket.reload(tour.id)
              updateTournamentStanding(tour)
              withdrawNonMover(game)

  private def updatePlayerAfterGame(tour: Tournament, game: Game, pairing: Pairing)(userId: UserId): Funit =
    tour.rated.yes
      .so:
        userApi.perfOptionOf(userId, tour.perfType)
      .flatMap: perf =>
        playerRepo.update(tour.id, userId): player =>
          for
            sheet <- cached.sheet.addResult(tour, userId, pairing)
            newPlayer = player.copy(
              score = sheet.total,
              fire = tour.streakable && sheet.isOnFire,
              rating = perf.fold(player.rating)(_.intRating),
              provisional = perf.fold(player.provisional)(_.provisional),
              performance = {
                for
                  performance <- performanceOf(game, userId).map(_.value.toDouble)
                  nbGames = sheet.scores.size
                  if nbGames > 0
                yield IntRating:
                  Math.round {
                    (player.performance.so(_.value) * (nbGames - 1) + performance) / nbGames
                  }.toInt
              }.orElse(player.performance)
            )
            _ = game.whitePlayer.userId.foreach: whiteUserId =>
              colorHistoryApi.inc(player.id, Color.fromWhite(player.is(whiteUserId)))
          yield newPlayer

  private def performanceOf(g: Game, userId: UserId): Option[IntRating] = for
    opponent <- g.opponentOf(userId)
    opponentRating <- opponent.rating
    multiplier = g.winnerUserId.so(winner => if winner == userId then 1 else -1)
  yield opponentRating.map(_ + 500 * multiplier)

  private def withdrawNonMover(game: Game): Unit =
    if game.status == chess.Status.NoStart then
      for
        tourId <- game.tournamentId
        player <- game.playerWhoDidNotMove
        userId <- player.userId
      yield withdraw(tourId, userId, isPause = false, isStalling = false)

  def pausePlaybanned(userId: UserId) =
    tournamentRepo
      .withdrawableIds(userId, reason = "pausePlaybanned")
      .flatMap:
        _.sequentiallyVoid(playerRepo.withdraw(_, userId))

  def isForBots(tourId: TourId): Fu[Boolean] =
    for tour <- cached.tourCache.byId(tourId)
    yield tour.exists(_.conditions.allowsBots)

  private[tournament] def kickFromTeam(teamId: TeamId, userId: UserId): Funit =
    tournamentRepo
      .withdrawableIds(userId, teamId = teamId.some, reason = "kickFromTeam")
      .flatMap:
        _.sequentiallyVoid: tourId =>
          Parallel(tourId, "kickFromTeam")(tournamentRepo.byId): tour =>
            for
              _ <-
                if tour.isCreated then playerRepo.remove(tour.id, userId)
                else playerRepo.withdraw(tour.id, userId)
              _ <- updateNbPlayers(tourId)
            yield
              duelStore.kick(tour, userId)
              socket.reload(tourId)

  // withdraws the player and forfeits all pairings in ongoing tournaments
  private[tournament] def ejectLameFromEnterable(tourId: TourId, userId: UserId): Funit =
    Parallel(tourId, "ejectLameFromEnterable")(cached.tourCache.enterable): tour =>
      if tour.isCreated then playerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id)
      else
        for
          _ <- playerRepo.remove(tourId, userId)
          _ <- tour.isStarted.so:
            for
              pairing <- pairingRepo.findPlaying(tour.id, userId)
              _ = pairing.foreach: currentPairing =>
                roundApi.tell(currentPairing.gameId, RoundBus.AbortForce)
              uids <- pairingRepo.opponentsOf(tour.id, userId)
              _ <- pairingRepo.forfeitByTourAndUserId(tour.id, userId)
              _ <- uids.toList.sequentiallyVoid(recomputePlayerAndSheet(tour))
            yield ()
          _ <- updateNbPlayers(tour.id)
        yield
          duelStore.kick(tour, userId)
          socket.reload(tour.id)
          publish()

  private def recomputePlayerAndSheet(tour: Tournament)(userId: UserId): Funit =
    tour.rated.yes.so { userApi.perfOptionOf(userId, tour.perfType) }.flatMap { perf =>
      playerRepo.update(tour.id, userId): player =>
        cached.sheet.recompute(tour, userId).map { sheet =>
          player.copy(
            score = sheet.total,
            fire = tour.streakable && sheet.isOnFire,
            rating = perf.fold(player.rating)(_.intRating),
            provisional = perf.fold(player.provisional)(_.provisional)
          )
        }
    }

  private[tournament] def recomputeEntireTournament(id: TourId): Funit =
    tournamentRepo.byId(id).flatMapz { tour =>
      playerRepo
        .sortedCursor(tour.id, 64, _.pri)
        .documentSource()
        .mapAsyncUnordered(4): player =>
          cached.sheet.recompute(tour, player.userId).dmap(player -> _)
        .mapAsyncUnordered(4): (player, sheet) =>
          playerRepo.update:
            player.copy(
              score = sheet.total,
              fire = tour.streakable && sheet.isOnFire
            )
        .run()
        .void
    }

  // erases player from tournament and reassigns winner
  private[tournament] def removePlayerAndRewriteHistory(tourId: TourId, userId: UserId): Funit =
    Parallel(tourId, "removePlayerAndRewriteHistory")(tournamentRepo.finishedById): tour =>
      for
        _ <- playerRepo.remove(tourId, userId)
        _ <- tour.winnerId
          .contains(userId)
          .so:
            playerRepo.winner(tour.id).flatMapz { p =>
              tournamentRepo.setWinnerId(tour.id, p.userId)
            }
      yield ()

  private val tournamentTopNb = 20
  private val tournamentTopCache = cacheApi[TourId, TournamentTop](16, "tournament.top"):
    _.refreshAfterWrite(3.second)
      .expireAfterAccess(5.minutes)
      .maximumSize(64)
      .buildAsyncFuture: id =>
        playerRepo.bestByTour(id, tournamentTopNb).dmap(TournamentTop.apply)

  def tournamentTop(tourId: TourId): Fu[TournamentTop] =
    tournamentTopCache.get(tourId)

  object gameView:

    private def OfGame[A](game: Game)(f: => Tournament => Fu[A]): Fu[Option[A]] =
      game.tournamentId.so(get).flatMap(_.traverse(f))

    def player(pov: Pov): Fu[Option[GameView]] =
      OfGame(pov.game): tour =>
        (getTeamVs(tour, pov.game), getGameRanks(tour, pov.game)).flatMapN { (teamVs, ranks) =>
          teamVs
            .fold(tournamentTop(tour.id)): vs =>
              cached.teamInfo.get(tour.id -> vs.teams(pov.color)).map { info =>
                TournamentTop(info.topPlayers.take(tournamentTopNb))
              }
            .dmap: top =>
              GameView(tour, teamVs, ranks, top.some)
        }

    def watcher(game: Game): Fu[Option[GameView]] =
      OfGame(game): tour =>
        getTeamVs(tour, game)
          .zip(getGameRanks(tour, game))
          .dmap: (teamVs, ranks) =>
            GameView(tour, teamVs, ranks, none)

    def mobile(game: Game): Fu[Option[GameView]] =
      OfGame(game): tour =>
        getGameRanks(tour, game).dmap(GameView(tour, none, _, none))

    def analysis(game: Game): Fu[Option[GameView]] =
      OfGame(game): tour =>
        getTeamVs(tour, game).dmap(GameView(tour, _, none, none))

    def withTeamVs(game: Game): Fu[Option[TourAndTeamVs]] =
      OfGame(game): tour =>
        getTeamVs(tour, game).dmap(TourAndTeamVs(tour, _))

    def getGameRanks(tour: lila.core.tournament.Tournament, game: Game): Fu[Option[ByColor[Rank]]] =
      game.whitePlayer.userId.ifTrue(tour.isStarted).so { whiteId =>
        game.blackPlayer.userId.so: blackId =>
          cached.ranking(tour).map { ranking =>
            (ranking.ranking.get(whiteId), ranking.ranking.get(blackId)).mapN: (whiteR, blackR) =>
              ByColor(whiteR + 1, blackR + 1)
          }
      }

    private def getTeamVs(tour: Tournament, game: Game): Fu[Option[TeamBattle.TeamVs]] =
      tour.isTeamBattle.so(playerRepo.teamVs(tour.id, game))

  def notableFinished = cached.notableFinishedCache.get {}

  private def scheduledCreatedAndStarted =
    tournamentRepo.scheduledCreated(6 * 60).zip(tournamentRepo.scheduledStarted)

  // when loading /tournament
  def fetchVisibleTournaments: Fu[VisibleTournaments] =
    scheduledCreatedAndStarted.zip(notableFinished).map { case ((created, started), finished) =>
      VisibleTournaments(created, started, finished)
    }

  // when updating /tournament
  def fetchUpdateTournaments: Fu[VisibleTournaments] =
    scheduledCreatedAndStarted.dmap: (created, started) =>
      VisibleTournaments(created, started, Nil)

  def fetchModable: Fu[List[lila.core.tournament.Tournament]] =
    fetchVisibleTournaments.map(_.all)

  def playerInfo(tour: Tournament, userId: UserId): Fu[Option[PlayerInfoExt]] =
    playerRepo.find(tour.id, userId).flatMapz { player =>
      playerPovs(tour, userId, 50).map: povs =>
        PlayerInfoExt(userId, player, povs).some
    }

  def allCurrentLeadersInStandard: Fu[Map[lila.core.tournament.Tournament, List[UserId]]] =
    tournamentRepo.standardPublicStartedFromSecondary.flatMap:
      _.sequentially: tour =>
        tournamentTop(tour.id).dmap(tour -> _.value.map(_.userId))
      .dmap(_.toMap)

  def calendar: Fu[List[Tournament]] =
    val from = nowInstant.minusDays(1)
    tournamentRepo.calendar(from = from, to = from.plusYears(1))

  def history(freq: Schedule.Freq, page: Int): Fu[Paginator[Tournament]] =
    Paginator(
      adapter = tournamentRepo.finishedByFreqAdapter(freq),
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )

  def resultStream(
      tour: Tournament,
      perSecond: MaxPerSecond,
      nb: Int,
      withSheet: Boolean
  ): Source[Player.Result, ?] =
    playerRepo
      .sortedCursor(tour.id, perSecond.value)
      .documentSource(nb)
      .throttle(perSecond.value, 1.second)
      .mapAsync(1): player =>
        withSheet.optionFu(cached.sheet(tour, player.userId)).dmap(player -> _)
      .zipWithIndex
      .mapAsync(8) { case ((player, sheet), index) =>
        lightUserApi
          .asyncFallback(player.userId)
          .map:
            Player.Result(player, _, index.toInt + 1, sheet)
      }

  def byOwnerStream(
      owner: User,
      status: List[Status],
      perSecond: MaxPerSecond,
      nb: Int
  ): Source[Tournament, ?] =
    tournamentRepo
      .sortedCursor(owner, status, perSecond.value)
      .documentSource(nb)
      .throttle(perSecond.value, 1.second)

  def byOwnerPager(owner: User, page: Int): Fu[Paginator[Tournament]] =
    Paginator(
      adapter = tournamentRepo.byOwnerAdapter(owner),
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )

  object upcomingByPlayerPager:

    private val max = 20

    private val cache =
      cacheApi[UserId, lila.db.paginator.StaticAdapter[Tournament]](64, "tournament.upcomingByPlayer"):
        _.expireAfterWrite(10.seconds).buildAsyncFuture:
          tournamentRepo.upcomingAdapterExpensiveCacheMe(_, max)

    def apply(player: User, page: Int): Fu[Paginator[Tournament]] =
      cache.get(player.id).flatMap { adapter =>
        Paginator(
          adapter = adapter,
          currentPage = page,
          maxPerPage = MaxPerPage(max)
        )
      }

  def visibleByTeam(teamId: TeamId, nbPast: Int, nbNext: Int): Fu[Tournament.PastAndNext] =
    tournamentRepo
      .finishedByTeam(teamId, nbPast)
      .zip(tournamentRepo.upcomingByTeam(teamId, nbNext))
      .map((Tournament.PastAndNext.apply).tupled)

  def toggleFeaturing(tourId: TourId, v: Boolean): Funit =
    if v then
      tournamentRepo.byId(tourId).flatMapz { tour =>
        tournamentRepo.setSchedule(tour.id, Scheduled(Schedule.Freq.Unique, tour.startsAt.dateTime).some)
      }
    else tournamentRepo.setSchedule(tourId, none)

  def onUserDelete(u: UserId) =
    leaderboard
      .byPlayerStream(u, withPerformance = false, perSecond = MaxPerSecond(100), nb = Int.MaxValue)
      .mapAsync(1): result =>
        import result.tour
        for
          _ <- tournamentRepo.anonymize(tour, u)
          // here we use a single ghost ID for all arena players and pairings,
          // because the mapping of arena player to arena pairings must be preserved
          ghostId = UserId(s"!${scalalib.ThreadLocalRandom.nextString(8)}")
          _ <- playerRepo.anonymize(tour.id, u)(ghostId)
          _ <- pairingRepo.anonymize(tour.id, u)(ghostId)
        yield ()
      .run()

  private def playerPovs(tour: Tournament, userId: UserId, nb: Int): Fu[List[LightPov]] =
    pairingRepo
      .recentIdsByTourAndUserId(tour.id, userId, nb)
      .flatMap(gameRepo.light.gamesFromPrimary)
      .map:
        _.flatMap { LightPov(_, userId) }

  private def Parallel[A: Zero](tourId: TourId, action: String)(
      fetch: TourId => Fu[Option[Tournament]]
  )(run: Tournament => Fu[A]): Fu[A] =
    fetch(tourId).flatMapz { tour =>
      if tour.nbPlayers > 3000
      then run(tour).chronometer.mon(_.tournament.action(tourId.value, action)).result
      else run(tour)
    }

  private object publish:
    private val debouncer = Debouncer[Unit](scheduler.scheduleOnce(15.seconds, _), 1): _ =>
      given play.api.i18n.Lang = lila.core.i18n.defaultLang
      fetchUpdateTournaments.flatMap(apiJsonView.apply).foreach { json =>
        Bus.pub:
          lila.core.socket.SendToFlag("tournament", Json.obj("t" -> "reload", "d" -> json))
      }
    def apply() = debouncer.push(())

  private object updateTournamentStanding:

    // last published top hashCode
    private val lastPublished = lila.memo.CacheApi.scaffeineNoScheduler
      .initialCapacity(16)
      .expireAfterWrite(2.minute)
      .build[TourId, Int]()

    private def publishNow(tourId: TourId) =
      tournamentTop(tourId).map { top =>
        val lastHash: Int = ~lastPublished.getIfPresent(tourId)
        if lastHash != top.hashCode then
          Bus.pub(
            lila.core.round.TourStanding(tourId, JsonView.top(top, lightUserApi.sync))
          )
          lastPublished.put(tourId, top.hashCode)
      }

    private val throttler = new lila.common.EarlyMultiThrottler[TourId](logger)

    def apply(tour: Tournament): Unit =
      if !tour.isTeamBattle then throttler(tour.id, 15.seconds) { publishNow(tour.id) }

private object TournamentApi:

  final class Callbacks(
      val clearJsonViewCache: Tournament => Unit,
      val clearWinnersCache: Tournament => Unit,
      val clearTrophyCache: Tournament => Unit,
      val indexLeaderboard: Tournament => Funit
  )
