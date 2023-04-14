package lila.tournament

import akka.stream.scaladsl.*
import com.roundeights.hasher.Algo
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import play.api.libs.json.*
import scala.util.chaining.*

import lila.common.config.{ MaxPerPage, MaxPerSecond }
import lila.common.paginator.Paginator
import lila.common.{ Bus, Debouncer }
import lila.game.{ Game, GameRepo, LightPov, Pov }
import lila.hub.LeaderTeam
import lila.round.actorApi.round.{ AbortForce, GoBerserk }
import lila.user.{ User, UserRepo }

final class TournamentApi(
    cached: TournamentCache,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    apiJsonView: ApiJsonView,
    autoPairing: AutoPairing,
    pairingSystem: arena.PairingSystem,
    callbacks: TournamentApi.Callbacks,
    socket: TournamentSocket,
    tellRound: lila.round.TellRound,
    roundSocket: lila.round.RoundSocket,
    trophyApi: lila.user.TrophyApi,
    colorHistoryApi: ColorHistoryApi,
    verify: Condition.Verify,
    duelStore: DuelStore,
    pause: Pause,
    waitingUsers: WaitingUsersApi,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    proxyRepo: lila.round.GameProxyRepo
)(using
    Executor,
    akka.actor.ActorSystem,
    Scheduler,
    akka.stream.Materializer
):

  export tournamentRepo.{ byId as get }

  def createTournament(
      setup: TournamentSetup,
      me: User,
      leaderTeams: List[LeaderTeam],
      andJoin: Boolean = true
  ): Fu[Tournament] =
    val tour = Tournament.make(
      by = Right(me),
      name = setup.name,
      clock = setup.clockConfig,
      minutes = setup.minutes,
      waitMinutes = setup.waitMinutes | TournamentForm.waitMinuteDefault,
      startDate = setup.startDate,
      mode = setup.realMode,
      password = setup.password,
      variant = setup.realVariant,
      position = setup.realPosition,
      berserkable = (setup.berserkable | true) && !setup.timeControlPreventsBerserk,
      streakable = setup.streakable | true,
      teamBattle = setup.teamBattleByTeam map TeamBattle.init,
      description = setup.description,
      hasChat = setup.hasChat | true
    ) pipe { tour =>
      tour.copy(conditions = setup.conditions.convert(tour.perfType, leaderTeams.view.map(_.pair).toMap))
    }
    tournamentRepo.insert(tour) >> {
      setup.teamBattleByTeam.orElse(tour.conditions.teamMember.map(_.teamId)).?? { teamId =>
        tournamentRepo.setForTeam(tour.id, teamId).void
      }
    } >> {
      (andJoin && !me.isBot && !me.lame) ?? join(
        tour.id,
        me,
        TournamentForm.TournamentJoin(setup.teamBattleByTeam, tour.password),
        getUserTeamIds = _ => fuccess(leaderTeams.map(_.id)),
        asLeader = false,
        none
      )
    } inject tour

  def update(old: Tournament, data: TournamentSetup, leaderTeams: List[LeaderTeam]): Fu[Tournament] =
    updateTour(old, data, data updateAll old, leaderTeams)

  def apiUpdate(old: Tournament, data: TournamentSetup, leaderTeams: List[LeaderTeam]): Fu[Tournament] =
    updateTour(old, data, data updatePresent old, leaderTeams)

  private def updateTour(
      old: Tournament,
      data: TournamentSetup,
      tour: Tournament,
      leaderTeams: List[LeaderTeam]
  ): Fu[Tournament] =
    val finalized = tour.copy(
      conditions = data.conditions
        .convert(tour.perfType, leaderTeams.view.map(_.pair).toMap)
        .copy(teamMember = old.conditions.teamMember), // can't change that
      mode = if (tour.position.isDefined) chess.Mode.Casual else tour.mode
    )
    tournamentRepo.update(finalized) >>- cached.tourCache.clear(tour.id) inject finalized

  def teamBattleUpdate(
      tour: Tournament,
      data: TeamBattle.DataForm.Setup,
      filterExistingTeamIds: Set[TeamId] => Fu[Set[TeamId]]
  ): Funit = for
    formTeamIds <- filterExistingTeamIds(data.potentialTeamIds.filterNot(TeamBattle.blacklist.contains))
    teamIds <-
      if !tour.isCreated
      then playerRepo.teamsWithPlayers(tour.id).map(_ ++ formTeamIds).map(_ take TeamBattle.maxTeams)
      else fuccess(formTeamIds)
    _ <- tournamentRepo.setTeamBattle(tour.id, TeamBattle(teamIds, data.nbLeaders))
    _ <- tour.isCreated ?? { playerRepo.removeNotInTeams(tour.id, teamIds) >> updateNbPlayers(tour.id) }
  yield
    cached.tourCache.clear(tour.id)
    socket.reload(tour.id)

  def teamBattleTeamInfo(tour: Tournament, teamId: TeamId): Fu[Option[TeamBattle.TeamInfo]] =
    tour.teamBattle.exists(_ teams teamId) ?? cached.teamInfo.get(tour.id -> teamId)

  private val hadPairings = lila.memo.ExpireSetMemo[TourId](1 hour)

  private[tournament] def makePairings(
      forTour: Tournament,
      users: WaitingUsers,
      smallTourNbActivePlayers: Option[Int]
  ): Funit =
    (users.size > 1 && (
      !hadPairings.get(forTour.id) ||
        users.haveWaitedEnough ||
        smallTourNbActivePlayers.exists(_ <= users.size * 1.5)
    )) ??
      Parallel(forTour.id, "makePairings")(cached.tourCache.started) { tour =>
        cached
          .ranking(tour)
          .mon(_.tournament.pairing.createRanking)
          .flatMap { ranking =>
            pairingSystem
              .createPairings(tour, users, ranking, smallTourNbActivePlayers)
              .mon(_.tournament.pairing.createPairings)
              .flatMap {
                case Nil => funit
                case pairings =>
                  pairingRepo.insert(pairings.map(_.pairing)) >>
                    pairings
                      .map { pairing =>
                        autoPairing(tour, pairing, ranking.ranking)
                          .mon(_.tournament.pairing.createAutoPairing)
                          .map { socket.startGame(tour.id, _) }
                      }
                      .parallel
                      .void
                      .mon(_.tournament.pairing.createInserts) >>- {
                      lila.mon.tournament.pairing.batchSize.record(pairings.size).unit
                      waitingUsers.registerPairedUsers(tour.id, pairings.view.flatMap(_.pairing.users).toSet)
                      socket.reload(tour.id)
                      hadPairings put tour.id
                      featureOneOf(tour, pairings, ranking.ranking).unit // do outside of queue
                    }
              }
          }
          .monSuccess(_.tournament.pairing.create)
          .chronometer
          .logIfSlow(100, logger)(_ => s"Pairings for https://lichess.org/tournament/${tour.id}")
          .result
      }

  private def featureOneOf(tour: Tournament, pairings: List[Pairing.WithPlayers], ranking: Ranking): Funit =
    import cats.syntax.all.*
    tour.featuredId.ifTrue(pairings.nonEmpty) ?? pairingRepo.byId map2
      RankedPairing(ranking) map (_.flatten) flatMap { curOption =>
        pairings.flatMap(p => RankedPairing(ranking)(p.pairing)).minimumByOption(_.bestRank.value) ?? {
          bestCandidate =>
            def switch = tournamentRepo.setFeaturedGameId(tour.id, bestCandidate.pairing.gameId)
            curOption.filter(_.pairing.playing) match
              case Some(current) if bestCandidate.bestRank < current.bestRank => switch
              case Some(_)                                                    => funit
              case _                                                          => switch
        }
      }

  private[tournament] def start(oldTour: Tournament): Funit =
    Parallel(oldTour.id, "start")(cached.tourCache.created) { tour =>
      tournamentRepo.setStatus(tour.id, Status.Started) >>- {
        cached.tourCache clear tour.id
        socket reload tour.id
        publish()
      }
    }

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
      pairingRepo count tour.id flatMap {
        case 0 => destroy(tour)
        case _ =>
          for
            _      <- tournamentRepo.setStatus(tour.id, Status.Finished)
            _      <- playerRepo unWithdraw tour.id
            _      <- pairingRepo removePlaying tour.id
            winner <- playerRepo winner tour.id
            _      <- winner.??(p => tournamentRepo.setWinnerId(tour.id, p.userId))
          yield
            cached.tourCache clear tour.id
            callbacks.clearJsonViewCache(tour)
            socket.finish(tour.id)
            publish()
            playerRepo withPoints tour.id foreach {
              _ foreach { p =>
                userRepo.incToints(p.userId, p.score)
              }
            }
            awardTrophies(tour).logFailure(logger, _ => s"${tour.id} awardTrophies")
            callbacks.indexLeaderboard(tour).logFailure(logger, _ => s"${tour.id} indexLeaderboard")
            callbacks.clearWinnersCache(tour)
            callbacks.clearTrophyCache(tour)
            duelStore.remove(tour)
      }
    }

  private[tournament] val killSchedule = scala.collection.mutable.Set.empty[TourId]

  def kill(tour: Tournament): Funit =
    if (tour.isStarted) fuccess(killSchedule add tour.id).void
    else if (tour.isCreated) destroy(tour)
    else funit

  private def awardTrophies(tour: Tournament): Funit =
    import lila.user.TrophyKind.*
    import lila.tournament.Tournament.tournamentUrl
    tour.schedule.exists(_.freq == Schedule.Freq.Marathon) ?? {
      playerRepo.bestByTourWithRank(tour.id, 500).flatMap { players =>
        lila.common.LilaFuture
          .applySequentially(players) {
            case rp if rp.rank.value == 1 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonWinner)
            case rp if rp.rank <= 10 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopTen)
            case rp if rp.rank <= 50 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopFifty)
            case rp if rp.rank <= 100 =>
              trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopHundred)
            case rp => trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopFivehundred)
          }
          .void
      }
    }

  def getVerdicts(
      tour: Tournament,
      me: Option[User],
      getUserTeamIds: User => Fu[List[TeamId]],
      playerExists: Boolean
  ): Fu[Condition.All.WithVerdicts] =
    me match
      case None => fuccess(tour.conditions.accepted)
      case Some(user) =>
        if (tour.isStarted && playerExists) verify.rejoin(tour.conditions, user, getUserTeamIds)
        else verify(tour.conditions, user, getUserTeamIds)

  private[tournament] def join(
      tourId: TourId,
      me: User,
      data: TournamentForm.TournamentJoin,
      getUserTeamIds: User => Fu[List[TeamId]],
      asLeader: Boolean,
      promise: Option[Promise[Tournament.JoinResult]]
  ): Funit =
    Parallel(tourId, "join")(cached.tourCache.enterable) { tour =>
      playerRepo.find(tour.id, me.id) flatMap { prevPlayer =>
        import Tournament.JoinResult
        val fuResult: Fu[JoinResult] =
          if (
            prevPlayer.nonEmpty || tour.password.forall(p =>
              // plain text access code
              MessageDigest.isEqual(p.getBytes(UTF_8), (~data.password).getBytes(UTF_8)) ||
                // user-specific access code: HMAC-SHA256(access code, user id)
                MessageDigest
                  .isEqual(
                    Algo.hmac(p).sha256(me.id.value).hex.getBytes(UTF_8),
                    (~data.password).getBytes(UTF_8)
                  )
            )
          )
            getVerdicts(tour, me.some, getUserTeamIds, prevPlayer.isDefined) flatMap { verdicts =>
              if (!verdicts.accepted) fuccess(JoinResult.Verdicts)
              else if (!pause.canJoin(me.id, tour)) fuccess(JoinResult.Paused)
              else
                def proceedWithTeam(team: Option[TeamId]): Fu[JoinResult] =
                  playerRepo.join(tour.id, me, tour.perfType, team, prevPlayer) >>
                    updateNbPlayers(tour.id) >>- publish() inject JoinResult.Ok
                tour.teamBattle.fold(proceedWithTeam(none)) { battle =>
                  data.team match
                    case None if prevPlayer.isDefined => proceedWithTeam(none)
                    case Some(team) if battle.teams contains team =>
                      getUserTeamIds(me) flatMap { myTeams =>
                        if (myTeams has team) proceedWithTeam(team.some)
                        else fuccess(JoinResult.MissingTeam)
                      }
                    case _ => fuccess(JoinResult.MissingTeam)
                }
            }
          else
            fuccess(JoinResult.WrongEntryCode)
        fuResult map { result =>
          if (result.ok)
            data.team.ifTrue(asLeader && tour.isTeamBattle) foreach {
              tournamentRepo.setForTeam(tour.id, _)
            }
            if (~data.pairMeAsap) pairingRepo.isPlaying(tourId, me.id) foreach { isPlaying =>
              if !isPlaying then waitingUsers.addApiUser(tour, me)
            }
          socket.reload(tour.id)
          promise.foreach(_ success result)
        }
      }
    }

  def joinWithResult(
      tourId: TourId,
      me: User,
      data: TournamentForm.TournamentJoin,
      getUserTeamIds: User => Fu[List[TeamId]],
      isLeader: Boolean
  ): Fu[Tournament.JoinResult] =
    val promise = Promise[Tournament.JoinResult]()
    join(tourId, me, data, getUserTeamIds, isLeader, promise.some)
    promise.future.withTimeoutDefault(5.seconds, Tournament.JoinResult.Nope)

  def pageOf(tour: Tournament, userId: UserId): Fu[Option[Int]] =
    cached ranking tour map {
      _.ranking get userId map { rank =>
        rank.value / 10 + 1
      }
    }

  private object updateNbPlayers:
    private val onceEvery = lila.memo.OnceEvery[TourId](1 second)
    def apply(tourId: TourId): Funit = onceEvery(tourId) ?? {
      playerRepo count tourId flatMap { tournamentRepo.setNbPlayers(tourId, _) }
    }

  def selfPause(tourId: TourId, userId: UserId): Funit =
    withdraw(tourId, userId, isPause = true, isStalling = false)

  private def stallPause(tourId: TourId, userId: UserId): Funit =
    withdraw(tourId, userId, isPause = false, isStalling = true)

  private[tournament] def sittingDetected(game: Game, player: UserId): Funit =
    game.tournamentId ?? { stallPause(_, player) }

  private def withdraw(tourId: TourId, userId: UserId, isPause: Boolean, isStalling: Boolean): Funit =
    Parallel(tourId, "withdraw")(cached.tourCache.enterable) {
      case tour if tour.isCreated =>
        playerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id) >>- {
          socket.reload(tour.id)
          publish()
        }
      case tour if tour.isStarted =>
        for
          _ <- playerRepo.withdraw(tour.id, userId)
          pausable <-
            if isPause
            then cached.ranking(tour).map { _.ranking get userId exists (_ < 7) }
            else fuccess(isStalling)
        yield
          if (pausable) pause.add(userId)
          socket.reload(tour.id)
          publish()
      case _ => funit
    }

  def withdrawAll(user: User): Funit =
    tournamentRepo.withdrawableIds(user.id, reason = "withdrawAll") flatMap {
      _.map {
        withdraw(_, user.id, isPause = false, isStalling = false)
      }.parallel.void
    }

  private[tournament] def berserk(gameId: GameId, userId: UserId): Funit =
    proxyRepo game gameId flatMap {
      _.filter(_.berserkable) ?? { game =>
        game.tournamentId ?? { tourId =>
          pairingRepo.findPlaying(tourId, userId) flatMap {
            case Some(pairing) if !pairing.berserkOf(userId) =>
              (pairing colorOf userId) ?? { color =>
                roundSocket.rounds.ask(gameId) { GoBerserk(color, _) } flatMapz {
                  pairingRepo.setBerserk(pairing, userId)
                }
              }
            case _ => funit
          }
        }
      }
    }

  private[tournament] def finishGame(game: Game): Funit =
    game.tournamentId ?? { tourId =>
      pairingRepo.finishAndGet(game) flatMap { pairingOpt =>
        Parallel(tourId, "finishGame")(cached.tourCache.started) { tour =>
          pairingOpt ?? { pairing =>
            game.userIds.map(updatePlayerAfterGame(tour, game, pairing)).parallel.void
          } >>- {
            duelStore.remove(game)
            socket.reload(tour.id)
            updateTournamentStanding(tour)
            withdrawNonMover(game)
          }
        }
      }
    }

  private def updatePlayerAfterGame(tour: Tournament, game: Game, pairing: Pairing)(userId: UserId): Funit =
    tour.mode.rated ?? { userRepo.perfOf(userId, tour.perfType) } flatMap { perf =>
      playerRepo.update(tour.id, userId) { player =>
        cached.sheet.addResult(tour, userId, pairing).map { sheet =>
          player.copy(
            score = sheet.total,
            fire = tour.streakable && sheet.isOnFire,
            rating = perf.fold(player.rating)(_.intRating),
            provisional = perf.fold(player.provisional)(_.provisional),
            performance = {
              for
                performance <- performanceOf(game, userId).map(_.value.toDouble)
                nbGames = sheet.scores.size
                if nbGames > 0
              yield Math.round {
                (player.performance * (nbGames - 1) + performance) / nbGames
              }.toInt
            } | player.performance
          )
        } >>- game.whitePlayer.userId.foreach { whiteUserId =>
          colorHistoryApi.inc(player.id, chess.Color.fromWhite(player is whiteUserId))
        }
      }
    }

  private def performanceOf(g: Game, userId: UserId): Option[IntRating] = for
    opponent       <- g.opponentByUserId(userId)
    opponentRating <- opponent.rating
    multiplier = g.winnerUserId.??(winner => if (winner == userId) 1 else -1)
  yield opponentRating + 500 * multiplier

  private def withdrawNonMover(game: Game): Unit =
    if (game.status == chess.Status.NoStart) for
      tourId <- game.tournamentId
      player <- game.playerWhoDidNotMove
      userId <- player.userId
    yield withdraw(tourId, userId, isPause = false, isStalling = false)

  def pausePlaybanned(userId: UserId) =
    tournamentRepo.withdrawableIds(userId, reason = "pausePlaybanned") flatMap {
      _.map {
        playerRepo.withdraw(_, userId)
      }.parallel.void
    }

  private[tournament] def kickFromTeam(teamId: TeamId, userId: UserId): Funit =
    tournamentRepo.withdrawableIds(userId, teamId = teamId.some, reason = "kickFromTeam") flatMap {
      _.map { tourId =>
        Parallel(tourId, "kickFromTeam")(tournamentRepo.byId) { tour =>
          val fu =
            if (tour.isCreated) playerRepo.remove(tour.id, userId)
            else playerRepo.withdraw(tour.id, userId)
          fu >> updateNbPlayers(tourId) >>- socket.reload(tourId)
        }
      }.parallel.void
    }

  // withdraws the player and forfeits all pairings in ongoing tournaments
  private[tournament] def ejectLameFromEnterable(tourId: TourId, userId: UserId): Funit =
    Parallel(tourId, "ejectLameFromEnterable")(cached.tourCache.enterable) { tour =>
      if (tour.isCreated)
        playerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id)
      else
        playerRepo.remove(tourId, userId) >> {
          tour.isStarted ?? {
            pairingRepo.findPlaying(tour.id, userId).map {
              _ foreach { currentPairing =>
                tellRound(currentPairing.gameId, AbortForce)
              }
            } >> pairingRepo.opponentsOf(tour.id, userId).flatMap { uids =>
              pairingRepo.forfeitByTourAndUserId(tour.id, userId) >>
                lila.common.LilaFuture.applySequentially(uids.toList)(recomputePlayerAndSheet(tour))
            }
          }
        } >>
          updateNbPlayers(tour.id) >>-
          socket.reload(tour.id) >>- publish()
    }

  private def recomputePlayerAndSheet(tour: Tournament)(userId: UserId): Funit =
    tour.mode.rated ?? { userRepo.perfOf(userId, tour.perfType) } flatMap { perf =>
      playerRepo.update(tour.id, userId) { player =>
        cached.sheet.recompute(tour, userId).map { sheet =>
          player.copy(
            score = sheet.total,
            fire = tour.streakable && sheet.isOnFire,
            rating = perf.fold(player.rating)(_.intRating),
            provisional = perf.fold(player.provisional)(_.provisional)
          )
        }
      }
    }

  private[tournament] def recomputeEntireTournament(id: TourId): Funit =
    tournamentRepo.byId(id) flatMapz { tour =>
      import reactivemongo.api.ReadPreference
      playerRepo
        .sortedCursor(tour.id, 64, ReadPreference.primary)
        .documentSource()
        .mapAsyncUnordered(4) { player =>
          cached.sheet.recompute(tour, player.userId) dmap (player -> _)
        }
        .mapAsyncUnordered(4) { case (player, sheet) =>
          playerRepo.update(
            player.copy(
              score = sheet.total,
              fire = tour.streakable && sheet.isOnFire
            )
          )
        }
        .toMat(Sink.ignore)(Keep.right)
        .run()
        .void
    }

  // erases player from tournament and reassigns winner
  private[tournament] def removePlayerAndRewriteHistory(tourId: TourId, userId: UserId): Funit =
    Parallel(tourId, "removePlayerAndRewriteHistory")(tournamentRepo.finishedById) { tour =>
      playerRepo.remove(tourId, userId) >> {
        tour.winnerId.contains(userId) ?? {
          playerRepo winner tour.id flatMapz { p =>
            tournamentRepo.setWinnerId(tour.id, p.userId)
          }
        }
      }
    }

  private val tournamentTopNb = 20
  private val tournamentTopCache = cacheApi[TourId, TournamentTop](16, "tournament.top") {
    _.refreshAfterWrite(3 second)
      .expireAfterAccess(5 minutes)
      .maximumSize(64)
      .buildAsyncFuture { id =>
        playerRepo.bestByTour(id, tournamentTopNb) dmap TournamentTop.apply
      }
  }

  def tournamentTop(tourId: TourId): Fu[TournamentTop] =
    tournamentTopCache get tourId

  object gameView:

    def player(pov: Pov): Fu[Option[GameView]] =
      (pov.game.tournamentId ?? get) flatMapz { tour =>
        getTeamVs(tour, pov.game) zip getGameRanks(tour, pov.game) flatMap { case (teamVs, ranks) =>
          teamVs.fold(tournamentTop(tour.id) dmap some) { vs =>
            cached.teamInfo.get(tour.id -> vs.teams(pov.color)) map2 { info =>
              TournamentTop(info.topPlayers take tournamentTopNb)
            }
          } dmap {
            GameView(tour, teamVs, ranks, _).some
          }
        }
      }

    def watcher(game: Game): Fu[Option[GameView]] =
      (game.tournamentId ?? get) flatMapz { tour =>
        getTeamVs(tour, game) zip getGameRanks(tour, game) dmap { case (teamVs, ranks) =>
          GameView(tour, teamVs, ranks, none).some
        }
      }

    def mobile(game: Game): Fu[Option[GameView]] =
      (game.tournamentId ?? get) flatMapz { tour =>
        getGameRanks(tour, game) dmap { ranks =>
          GameView(tour, none, ranks, none).some
        }
      }

    def analysis(game: Game): Fu[Option[GameView]] =
      (game.tournamentId ?? get) flatMapz { tour =>
        getTeamVs(tour, game) dmap { GameView(tour, _, none, none).some }
      }

    def withTeamVs(game: Game): Fu[Option[TourAndTeamVs]] =
      (game.tournamentId ?? get) flatMapz { tour =>
        getTeamVs(tour, game) dmap { TourAndTeamVs(tour, _).some }
      }

    private def getGameRanks(tour: Tournament, game: Game): Fu[Option[GameRanks]] =
      game.whitePlayer.userId.ifTrue(tour.isStarted) ?? { whiteId =>
        game.blackPlayer.userId ?? { blackId =>
          cached ranking tour map { ranking =>
            import cats.syntax.all.*
            (ranking.ranking.get(whiteId), ranking.ranking.get(blackId)) mapN { (whiteR, blackR) =>
              GameRanks(whiteR + 1, blackR + 1)
            }
          }
        }
      }

    private def getTeamVs(tour: Tournament, game: Game): Fu[Option[TeamBattle.TeamVs]] =
      tour.isTeamBattle ?? playerRepo.teamVs(tour.id, game)

  def notableFinished = cached.notableFinishedCache.get {}

  private def scheduledCreatedAndStarted =
    tournamentRepo.scheduledCreated(6 * 60) zip tournamentRepo.scheduledStarted

  // when loading /tournament
  def fetchVisibleTournaments: Fu[VisibleTournaments] =
    scheduledCreatedAndStarted zip notableFinished map { case ((created, started), finished) =>
      VisibleTournaments(created, started, finished)
    }

  // when updating /tournament
  def fetchUpdateTournaments: Fu[VisibleTournaments] =
    scheduledCreatedAndStarted dmap { (created, started) =>
      VisibleTournaments(created, started, Nil)
    }

  def playerInfo(tour: Tournament, userId: UserId): Fu[Option[PlayerInfoExt]] =
    playerRepo.find(tour.id, userId) flatMapz { player =>
      playerPovs(tour, userId, 50) map { povs =>
        PlayerInfoExt(userId, player, povs).some
      }
    }

  def allCurrentLeadersInStandard: Fu[Map[Tournament, TournamentTop]] =
    tournamentRepo.standardPublicStartedFromSecondary.flatMap {
      _.map { tour =>
        tournamentTop(tour.id) dmap (tour -> _)
      }.parallel
        .dmap(_.toMap)
    }

  def calendar: Fu[List[Tournament]] =
    val from = nowInstant.minusDays(1)
    tournamentRepo.calendar(from = from, to = from plusYears 1)

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
      .throttle(perSecond.value, 1 second)
      .mapAsync(1) { player =>
        withSheet.??(cached.sheet(tour, player.userId) dmap some).dmap(player -> _)
      }
      .zipWithIndex
      .mapAsync(8) { case ((player, sheet), index) =>
        lightUserApi.asyncFallback(player.userId) map {
          Player.Result(player, _, index.toInt + 1, sheet)
        }
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
      .throttle(perSecond.value, 1 second)

  def byOwnerPager(owner: User, page: Int): Fu[Paginator[Tournament]] =
    Paginator(
      adapter = tournamentRepo.byOwnerAdapter(owner),
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )

  object upcomingByPlayerPager:

    private val max = 20

    private val cache =
      cacheApi[UserId, lila.db.paginator.StaticAdapter[Tournament]](64, "tournament.upcomingByPlayer") {
        _.expireAfterWrite(10 seconds)
          .buildAsyncFuture {
            tournamentRepo.upcomingAdapterExpensiveCacheMe(_, max)
          }
      }

    def apply(player: User, page: Int): Fu[Paginator[Tournament]] =
      cache.get(player.id) flatMap { adapter =>
        Paginator(
          adapter = adapter,
          currentPage = page,
          maxPerPage = MaxPerPage(max)
        )
      }

  def visibleByTeam(teamId: TeamId, nbPast: Int, nbNext: Int): Fu[Tournament.PastAndNext] =
    tournamentRepo.finishedByTeam(teamId, nbPast) zip
      tournamentRepo.upcomingByTeam(teamId, nbNext) map
      (Tournament.PastAndNext.apply).tupled

  def toggleFeaturing(tourId: TourId, v: Boolean): Funit =
    if (v)
      tournamentRepo.byId(tourId) flatMapz { tour =>
        tournamentRepo.setSchedule(tour.id, Schedule.uniqueFor(tour).some)
      }
    else
      tournamentRepo.setSchedule(tourId, none)

  private def playerPovs(tour: Tournament, userId: UserId, nb: Int): Fu[List[LightPov]] =
    pairingRepo.recentIdsByTourAndUserId(tour.id, userId, nb) flatMap
      gameRepo.light.gamesFromPrimary map {
        _ flatMap { LightPov.ofUserId(_, userId) }
      }

  private def Parallel(tourId: TourId, action: String)(
      fetch: TourId => Fu[Option[Tournament]]
  )(run: Tournament => Funit): Funit =
    fetch(tourId) flatMapz { tour =>
      if (tour.nbPlayers > 1000)
        run(tour).chronometer.mon(_.tournament.action(tourId.value, action)).result
      else
        run(tour)
    }

  private object publish:
    private val debouncer = Debouncer[Unit](15 seconds, 1) { _ =>
      given play.api.i18n.Lang = lila.i18n.defaultLang
      fetchUpdateTournaments flatMap apiJsonView.apply foreach { json =>
        Bus.publish(
          lila.socket.SendToFlag("tournament", Json.obj("t" -> "reload", "d" -> json)),
          "sendToFlag"
        )
      }
    }
    def apply() = debouncer.push(()).unit

  private object updateTournamentStanding:

    // last published top hashCode
    private val lastPublished = lila.memo.CacheApi.scaffeineNoScheduler
      .initialCapacity(16)
      .expireAfterWrite(2 minute)
      .build[TourId, Int]()

    private def publishNow(tourId: TourId) =
      tournamentTop(tourId) map { top =>
        val lastHash: Int = ~lastPublished.getIfPresent(tourId)
        if (lastHash != top.hashCode)
          Bus.publish(
            lila.hub.actorApi.round.TourStanding(tourId, JsonView.top(top, lightUserApi.sync)),
            "tourStanding"
          )
          lastPublished.put(tourId, top.hashCode)
      }

    private val throttler = new lila.hub.EarlyMultiThrottler[TourId](logger)

    def apply(tour: Tournament): Unit =
      if (!tour.isTeamBattle) throttler(tour.id, 15.seconds) { publishNow(tour.id) }

private object TournamentApi:

  case class Callbacks(
      clearJsonViewCache: Tournament => Unit,
      clearWinnersCache: Tournament => Unit,
      clearTrophyCache: Tournament => Unit,
      indexLeaderboard: Tournament => Funit
  )
