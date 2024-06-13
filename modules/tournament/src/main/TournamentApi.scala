package lila.tournament

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.stream.scaladsl._
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.util.chaining._

import lila.common.config.{ MaxPerPage, MaxPerSecond }
import lila.common.paginator.Paginator
import lila.common.{ Bus, Debouncer, LightUser }
import lila.game.{ Game, GameRepo, LightPov, Pov }
import lila.hub.actorApi.lobby.ReloadTournaments
import lila.hub.LightTeam
import lila.hub.LightTeam._
import lila.round.actorApi.round.{ AbortForce, GoBerserk }
import lila.socket.Socket.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short

final class TournamentApi(
    cached: Cached,
    userRepo: UserRepo,
    gameRepo: GameRepo,
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    apiJsonView: ApiJsonView,
    autoPairing: AutoPairing,
    pairingSystem: arena.PairingSystem,
    callbacks: TournamentApi.Callbacks,
    renderer: lila.hub.actors.Renderer,
    socket: TournamentSocket,
    tellRound: lila.round.TellRound,
    roundSocket: lila.round.RoundSocket,
    trophyApi: lila.user.TrophyApi,
    verify: Condition.Verify,
    duelStore: DuelStore,
    pause: Pause,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi,
    proxyRepo: lila.round.GameProxyRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem,
    mode: play.api.Mode
) {

  private val workQueue =
    new lila.hub.DuctSequencers(
      maxSize = 256,
      expiration = 1 minute,
      timeout = 10 seconds,
      name = "tournament"
    )

  def get(id: Tournament.ID) = tournamentRepo byId id

  def createTournament(
      setup: TournamentSetup,
      me: User,
      myTeams: List[LightTeam],
      getUserTeamIds: User => Fu[List[TeamID]],
      andJoin: Boolean = true
  ): Fu[Tournament] = {
    val tour = Tournament.make(
      by = Right(me),
      name = setup.name,
      clock = setup.clockConfig,
      minutes = setup.minutes,
      waitMinutes = setup.waitMinutes | DataForm.waitMinuteDefault,
      startDate = setup.startDate,
      mode = setup.realMode,
      password = setup.password,
      variant = setup.realVariant,
      position = setup.position,
      berserkable = setup.berserkable | true,
      streakable = setup.streakable | true,
      teamBattle = setup.teamBattleByTeam map TeamBattle.init,
      description = setup.description,
      hasChat = setup.hasChat | true
    ) pipe { tour =>
      tour.perfType.fold(tour) { perfType =>
        tour.copy(conditions = setup.conditions.convert(perfType, myTeams.view.map(_.pair).toMap))
      }
    }
    tournamentRepo.insert(tour) >> {
      setup.teamBattleByTeam.orElse(tour.conditions.teamMember.map(_.teamId)).?? { teamId =>
        tournamentRepo.setForTeam(tour.id, teamId).void
      }
    } >> {
      andJoin ?? join(
        tour.id,
        me,
        tour.password,
        setup.teamBattleByTeam,
        getUserTeamIds,
        isLeader = false,
        none
      )
    } inject tour
  }

  def update(old: Tournament, data: TournamentSetup, myTeams: List[LightTeam]): Funit = {
    import data._
    val variant = if (old.isCreated) realVariant else old.variant
    val tour = old
      .copy(
        name = name | old.name,
        clock = if (old.isCreated) clockConfig else old.clock,
        minutes = minutes,
        mode = realMode,
        variant = variant,
        startsAt = startDate | old.startsAt,
        password = data.password,
        position = variant.standard ?? {
          if (old.isCreated || old.position.isDefined) data.position
          else old.position
        },
        noBerserk = !(~berserkable),
        noStreak = !(~streakable),
        teamBattle = old.teamBattle,
        description = description,
        hasChat = data.hasChat | true
      ) pipe { tour =>
      tour.perfType.fold(tour) { perfType =>
        tour.copy(
          conditions = conditions
            .convert(perfType, myTeams.view.map(_.pair).toMap)
            .copy(teamMember = old.conditions.teamMember), // can't change that
          mode = if (tour.position.isDefined) shogi.Mode.Casual else tour.mode
        )
      }
    }
    tournamentRepo update tour void
  }

  def teamBattleUpdate(
      tour: Tournament,
      data: TeamBattle.DataForm.Setup,
      filterExistingTeamIds: Set[TeamID] => Fu[Set[TeamID]]
  ): Funit =
    filterExistingTeamIds(data.potentialTeamIds) flatMap { teamIds =>
      tournamentRepo.setTeamBattle(tour.id, TeamBattle(teamIds, data.nbLeaders))
    }

  def teamBattleTeamInfo(tour: Tournament, teamId: TeamID): Fu[Option[TeamBattle.TeamInfo]] =
    tour.teamBattle.exists(_ teams teamId) ?? cached.teamInfo.get(tour.id -> teamId)

  private val hadPairings = new lila.memo.ExpireSetMemo(1 hour)

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
      Sequencing(forTour.id)(tournamentRepo.startedById) { tour =>
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
                  hadPairings put tour.id
                  playerRepo
                    .byTourAndUserIds(tour.id, pairings.flatMap(_.users))
                    .map {
                      _.view
                        .map { player =>
                          player.userId -> player
                        }
                        .toMap
                    }
                    .mon(_.tournament.pairing.createPlayerMap)
                    .flatMap { playersMap =>
                      pairings
                        .map { pairing =>
                          pairingRepo.insert(pairing) >>
                            autoPairing(tour, pairing, playersMap, ranking)
                              .mon(_.tournament.pairing.createAutoPairing)
                              .map { socket.startGame(tour.id, _) }
                        }
                        .sequenceFu
                        .mon(_.tournament.pairing.createInserts) >>
                        featureOneOf(tour, pairings, ranking)
                          .mon(_.tournament.pairing.createFeature) >>-
                        lila.mon.tournament.pairing.batchSize.record(pairings.size).unit
                    }
              }
          }
          .monSuccess(_.tournament.pairing.create)
          .chronometer
          .logIfSlow(100, logger)(_ => s"Pairings for https://lishogi.org/tournament/${tour.id}")
          .result
      }

  private def featureOneOf(tour: Tournament, pairings: Pairings, ranking: Ranking): Funit =
    tour.featuredId.ifTrue(pairings.nonEmpty) ?? pairingRepo.byId map2
      RankedPairing(ranking) map (_.flatten) flatMap { curOption =>
        pairings.flatMap(RankedPairing(ranking)).sortBy(_.bestRank).headOption ?? { bestCandidate =>
          def switch = tournamentRepo.setFeaturedGameId(tour.id, bestCandidate.pairing.gameId)
          curOption.filter(_.pairing.playing) match {
            case Some(current) if bestCandidate.bestRank < current.bestRank => switch
            case Some(_)                                                    => funit
            case _                                                          => switch
          }
        }
      }

  private[tournament] def start(oldTour: Tournament): Funit =
    Sequencing(oldTour.id)(tournamentRepo.createdById) { tour =>
      tournamentRepo.setStatus(tour.id, Status.Started) >>-
        socket.reload(tour.id) >>-
        publish()
    }

  private[tournament] def destroy(tour: Tournament): Funit =
    tournamentRepo.remove(tour).void >>
      pairingRepo.removeByTour(tour.id) >>
      playerRepo.removeByTour(tour.id) >>- publish() >>- socket.reload(tour.id)

  private[tournament] def finish(oldTour: Tournament): Funit =
    Sequencing(oldTour.id)(tournamentRepo.startedById) { tour =>
      pairingRepo count tour.id flatMap {
        case 0 => destroy(tour)
        case _ =>
          for {
            _      <- tournamentRepo.setStatus(tour.id, Status.Finished)
            _      <- playerRepo unWithdraw tour.id
            _      <- pairingRepo removePlaying tour.id
            winner <- playerRepo winner tour.id
            _      <- winner.??(p => tournamentRepo.setWinnerId(tour.id, p.userId))
          } yield {
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
    }

  def kill(tour: Tournament): Funit = {
    if (tour.isStarted) finish(tour)
    else if (tour.isCreated) destroy(tour)
    else funit
  }

  private def awardTrophies(tour: Tournament): Funit = {
    import lila.user.TrophyKind._
    import lila.tournament.Tournament.tournamentUrl
    tour.schedule.??(_.freq == Schedule.Freq.Marathon) ?? {
      playerRepo.bestByTourWithRank(tour.id, 100).flatMap {
        _.map {
          case rp if rp.rank == 1 => trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonWinner)
          case rp if rp.rank <= 10 =>
            trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopTen)
          case rp if rp.rank <= 50 =>
            trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopFifty)
          case rp => trophyApi.award(tournamentUrl(tour.id), rp.player.userId, marathonTopHundred)
        }.sequenceFu.void
      }
    }
  }

  def verdicts(
      tour: Tournament,
      me: Option[User],
      getUserTeamIds: User => Fu[List[TeamID]]
  ): Fu[Condition.All.WithVerdicts] =
    me match {
      case None => fuccess(tour.conditions.accepted)
      case Some(user) =>
        {
          tour.isStarted ?? playerRepo.exists(tour.id, user.id)
        } flatMap {
          case true => fuccess(tour.conditions.accepted)
          case _    => verify(tour.conditions, user, getUserTeamIds)
        }
    }

  private[tournament] def join(
      tourId: Tournament.ID,
      me: User,
      password: Option[String],
      withTeamId: Option[String],
      getUserTeamIds: User => Fu[List[TeamID]],
      isLeader: Boolean,
      promise: Option[Promise[Boolean]]
  ): Funit =
    Sequencing(tourId)(tournamentRepo.enterableById) { tour =>
      playerRepo.exists(tour.id, me.id) flatMap { playerExists =>
        val fuJoined =
          if (tour.password == password || playerExists) {
            verdicts(tour, me.some, getUserTeamIds) flatMap {
              _.accepted ?? {
                pause.canJoin(me.id, tour) ?? {
                  def proceedWithTeam(team: Option[String]) =
                    playerRepo.join(tour.id, me, tour.perfLens, team) >>
                      updateNbPlayers(tour.id) >>- {
                        socket.reload(tour.id)
                        publish()
                      } inject true
                  withTeamId match {
                    case None if tour.isTeamBattle => playerExists ?? proceedWithTeam(none)
                    case None                      => proceedWithTeam(none)
                    case Some(team) =>
                      tour.teamBattle match {
                        case Some(battle) if battle.teams contains team =>
                          getUserTeamIds(me) flatMap { myTeams =>
                            if (myTeams has team) proceedWithTeam(team.some)
                            // else proceedWithTeam(team.some) // listress
                            else fuccess(false)
                          }
                        case _ => fuccess(false)
                      }
                  }
                }
              }
            }
          } else {
            socket.reload(tour.id)
            fuccess(false)
          }
        fuJoined map { joined =>
          withTeamId.ifTrue(joined && isLeader && tour.isTeamBattle) foreach {
            tournamentRepo.setForTeam(tour.id, _)
          }
          promise.foreach(_ success joined)
        }
      }
    }

  def joinWithResult(
      tourId: Tournament.ID,
      me: User,
      password: Option[String],
      teamId: Option[String],
      getUserTeamIds: User => Fu[List[TeamID]],
      isLeader: Boolean
  ): Fu[Boolean] = {
    val promise = Promise[Boolean]()
    join(tourId, me, password, teamId, getUserTeamIds, isLeader, promise.some)
    promise.future.withTimeoutDefault(5.seconds, false)
  }

  def pageOf(tour: Tournament, userId: User.ID): Fu[Option[Int]] =
    cached ranking tour map {
      _ get userId map { rank =>
        rank / 10 + 1
      }
    }

  private def updateNbPlayers(tourId: Tournament.ID): Funit =
    playerRepo count tourId flatMap { tournamentRepo.setNbPlayers(tourId, _) }

  def selfPause(tourId: Tournament.ID, userId: User.ID): Funit =
    withdraw(tourId, userId, isPause = true, isStalling = false)

  private def stallPause(tourId: Tournament.ID, userId: User.ID): Funit =
    withdraw(tourId, userId, isPause = false, isStalling = true)

  private def withdraw(tourId: Tournament.ID, userId: User.ID, isPause: Boolean, isStalling: Boolean): Funit =
    Sequencing(tourId)(tournamentRepo.enterableById) {
      case tour if tour.isCreated =>
        playerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id) >>- socket.reload(
          tour.id
        ) >>- publish()
      case tour if tour.isStarted =>
        for {
          _ <- playerRepo.withdraw(tour.id, userId)
          pausable <-
            if (isPause) cached.ranking(tour).map { _ get userId exists (7 >) }
            else
              fuccess(isStalling)
        } yield {
          if (pausable) pause.add(userId)
          socket.reload(tour.id)
          publish()
        }
      case _ => funit
    }

  def withdrawAll(user: User): Funit =
    tournamentRepo.withdrawableIds(user.id) flatMap {
      _.map {
        withdraw(_, user.id, isPause = false, isStalling = false)
      }.sequenceFu.void
    }

  private[tournament] def berserk(gameId: Game.ID, userId: User.ID): Funit =
    proxyRepo game gameId flatMap {
      _.filter(_.berserkable) ?? { game =>
        game.tournamentId ?? { tourId =>
          Sequencing(tourId)(tournamentRepo.startedById) { tour =>
            pairingRepo.findPlaying(tour.id, userId) flatMap {
              case Some(pairing) if !pairing.berserkOf(userId) =>
                (pairing colorOf userId) ?? { color =>
                  roundSocket.rounds.ask(gameId) { GoBerserk(color, _) } flatMap {
                    _ ?? pairingRepo.setBerserk(pairing, userId)
                  }
                }
              case _ => funit
            }
          }
        }
      }
    }

  private[tournament] def finishGame(game: Game): Funit =
    game.tournamentId ?? { tourId =>
      Sequencing(tourId)(tournamentRepo.startedById) { tour =>
        pairingRepo.finish(game) >>
          game.userIds.map(updatePlayer(tour, game.some)).sequenceFu.void >>- {
            duelStore.remove(game)
            socket.reload(tour.id)
            updateTournamentStanding(tour)
            withdrawNonMover(game)
          }
      }
    }

  private[tournament] def sittingDetected(game: Game, player: User.ID): Funit =
    game.tournamentId ?? { stallPause(_, player) }

  private def updatePlayer(
      tour: Tournament,
      finishing: Option[
        Game
      ] // if set, update the player performance. Leave to none to just recompute the sheet.
  )(userId: User.ID): Funit =
    (tour.perfType.ifTrue(tour.mode.rated) ?? { userRepo.perfOf(userId, _) }) flatMap { perf =>
      playerRepo.update(tour.id, userId) { player =>
        cached.sheet.update(tour, userId) map { sheet =>
          player.copy(
            score = sheet.total,
            fire = tour.streakable && sheet.onFire,
            rating = perf.fold(player.rating)(_.intRating),
            provisional = perf.fold(player.provisional)(_.provisional),
            performance = {
              for {
                g           <- finishing
                performance <- performanceOf(g, userId).map(_.toDouble)
                nbGames = sheet.scores.size
                if nbGames > 0
              } yield Math.round {
                player.performance * (nbGames - 1) / nbGames + performance / nbGames
              } toInt
            } | player.performance
          )
        }
      }
    }

  private def performanceOf(g: Game, userId: String): Option[Int] =
    for {
      opponent       <- g.opponentByUserId(userId)
      opponentRating <- opponent.rating
      multiplier = g.winnerUserId.??(winner => if (winner == userId) 1 else -1)
    } yield opponentRating + 500 * multiplier

  private def withdrawNonMover(game: Game): Unit =
    if (game.status == shogi.Status.NoStart) for {
      tourId <- game.tournamentId
      player <- game.playerWhoDidNotMove
      userId <- player.userId
    } withdraw(tourId, userId, isPause = false, isStalling = false)

  def pausePlaybanned(userId: User.ID) =
    tournamentRepo.withdrawableIds(userId) flatMap {
      _.map {
        playerRepo.withdraw(_, userId)
      }.sequenceFu.void
    }

  private[tournament] def kickFromTeam(teamId: TeamID, userId: User.ID): Funit =
    tournamentRepo.withdrawableIds(userId, teamId = teamId.some) flatMap {
      _.map { tourId =>
        Sequencing(tourId)(tournamentRepo.byId) { tour =>
          val fu =
            if (tour.isCreated) playerRepo.remove(tour.id, userId)
            else playerRepo.withdraw(tour.id, userId)
          fu >> updateNbPlayers(tourId) >>- socket.reload(tourId)
        }
      }.sequenceFu.void
    }

  def ejectLame(userId: User.ID, playedIds: List[Tournament.ID]): Funit =
    tournamentRepo.withdrawableIds(userId) flatMap { tourIds =>
      (tourIds ::: playedIds).distinct
        .map {
          ejectLame(_, userId)
        }
        .sequenceFu
        .void
    }

  // withdraws the player and forfeits all pairings in ongoing tournaments
  private def ejectLame(tourId: Tournament.ID, userId: User.ID): Funit =
    Sequencing(tourId)(tournamentRepo.byId) { tour =>
      playerRepo.withdraw(tourId, userId) >> {
        if (tour.isStarted)
          pairingRepo.findPlaying(tour.id, userId).map {
            _ foreach { currentPairing =>
              tellRound(currentPairing.gameId, AbortForce)
            }
          } >> pairingRepo.opponentsOf(tour.id, userId).flatMap { uids =>
            pairingRepo.forfeitByTourAndUserId(tour.id, userId) >>
              lila.common.Future.applySequentially(uids.toList)(updatePlayer(tour, none))
          }
        else if (tour.isFinished && tour.winnerId.contains(userId))
          playerRepo winner tour.id flatMap {
            _ ?? { p =>
              tournamentRepo.setWinnerId(tour.id, p.userId)
            }
          }
        else funit
      } >>
        updateNbPlayers(tour.id) >>-
        socket.reload(tour.id) >>- publish()
    }

  private val tournamentTopNb = 20
  private val tournamentTopCache = cacheApi[Tournament.ID, TournamentTop](16, "tournament.top") {
    _.refreshAfterWrite(3 second)
      .expireAfterAccess(5 minutes)
      .maximumSize(64)
      .buildAsyncFuture { id =>
        playerRepo.bestByTour(id, tournamentTopNb) dmap TournamentTop.apply
      }
  }

  def tournamentTop(tourId: Tournament.ID): Fu[TournamentTop] =
    tournamentTopCache get tourId

  object gameView {

    def player(pov: Pov): Fu[Option[GameView]] =
      (pov.game.tournamentId ?? get) flatMap {
        _ ?? { tour =>
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
      }

    def watcher(game: Game): Fu[Option[GameView]] =
      (game.tournamentId ?? get) flatMap {
        _ ?? { tour =>
          getTeamVs(tour, game) zip getGameRanks(tour, game) dmap { case (teamVs, ranks) =>
            GameView(tour, teamVs, ranks, none).some
          }
        }
      }

    def mobile(game: Game): Fu[Option[GameView]] =
      (game.tournamentId ?? get) flatMap {
        _ ?? { tour =>
          getGameRanks(tour, game) dmap { ranks =>
            GameView(tour, none, ranks, none).some
          }
        }
      }

    def analysis(game: Game): Fu[Option[GameView]] =
      (game.tournamentId ?? get) flatMap {
        _ ?? { tour =>
          getTeamVs(tour, game) dmap { GameView(tour, _, none, none).some }
        }
      }

    def withTeamVs(game: Game): Fu[Option[TourAndTeamVs]] =
      (game.tournamentId ?? get) flatMap {
        _ ?? { tour =>
          getTeamVs(tour, game) dmap { TourAndTeamVs(tour, _).some }
        }
      }

    private def getGameRanks(tour: Tournament, game: Game): Fu[Option[GameRanks]] =
      ~ {
        game.sentePlayer.userId.ifTrue(tour.isStarted) flatMap { senteId =>
          game.gotePlayer.userId map { goteId =>
            cached ranking tour map { ranking =>
              import cats.implicits._
              (ranking.get(senteId), ranking.get(goteId)) mapN { (senteR, goteR) =>
                GameRanks(senteR + 1, goteR + 1)
              }
            }
          }
        }
      }

    private def getTeamVs(tour: Tournament, game: Game): Fu[Option[TeamBattle.TeamVs]] =
      tour.isTeamBattle ?? playerRepo.teamVs(tour.id, game)
  }

  def notableFinished = cached.notableFinishedCache.get {}

  private def createdAndStarted =
    tournamentRepo.created(8 * 60) zip tournamentRepo.started

  // when loading /tournament
  def fetchVisibleTournaments: Fu[VisibleTournaments] =
    createdAndStarted zip notableFinished map { case ((created, started), finished) =>
      VisibleTournaments(created, started, finished)
    }

  // when updating /tournament
  def fetchUpdateTournaments: Fu[VisibleTournaments] =
    createdAndStarted dmap { case (created, started) =>
      VisibleTournaments(created, started, Nil)
    }

  def playerInfo(tour: Tournament, userId: User.ID): Fu[Option[PlayerInfoExt]] =
    playerRepo.find(tour.id, userId) flatMap {
      _ ?? { player =>
        playerPovs(tour, userId, 50) map { povs =>
          PlayerInfoExt(userId, player, povs).some
        }
      }
    }

  def allCurrentLeadersInStandard: Fu[Map[Tournament, TournamentTop]] =
    tournamentRepo.standardPublicStartedFromSecondary.flatMap {
      _.map { tour =>
        tournamentTop(tour.id) dmap (tour -> _)
      }.sequenceFu
        .dmap(_.toMap)
    }

  def calendar: Fu[List[Tournament]] = {
    val from = DateTime.now.minusDays(1)
    tournamentRepo.calendar(from = from, to = from plusYears 1)
  }

  def resultStream(tour: Tournament, perSecond: MaxPerSecond, nb: Int): Source[Player.Result, _] =
    playerRepo
      .sortedCursor(tour.id, perSecond.value)
      .documentSource(nb)
      .throttle(perSecond.value, 1 second)
      .zipWithIndex
      .mapAsync(8) { case (player, index) =>
        lightUserApi.async(player.userId) map { lu =>
          Player.Result(player, lu | LightUser.fallback(player.userId), index.toInt + 1)
        }
      }

  def byOwnerStream(owner: User, perSecond: MaxPerSecond, nb: Int): Source[Tournament, _] =
    tournamentRepo
      .sortedCursor(owner, perSecond.value)
      .documentSource(nb)
      .throttle(perSecond.value, 1 second)

  def byOwnerPager(owner: User, page: Int): Fu[Paginator[Tournament]] =
    Paginator(
      adapter = tournamentRepo.byOwnerAdapter(owner),
      currentPage = page,
      maxPerPage = MaxPerPage(20)
    )

  def visibleByTeam(teamId: TeamID, nbPast: Int, nbNext: Int): Fu[Tournament.PastAndNext] =
    tournamentRepo.finishedByTeam(teamId, nbPast) zip
      tournamentRepo.upcomingByTeam(teamId, nbNext) map
      (Tournament.PastAndNext.apply _).tupled

  private def playerPovs(tour: Tournament, userId: User.ID, nb: Int): Fu[List[LightPov]] =
    pairingRepo.recentIdsByTourAndUserId(tour.id, userId, nb) flatMap
      gameRepo.light.gamesFromPrimary map {
        _ flatMap { LightPov.ofUserId(_, userId) }
      }

  private def Sequencing(
      tourId: Tournament.ID
  )(fetch: Tournament.ID => Fu[Option[Tournament]])(run: Tournament => Funit): Funit =
    workQueue(tourId) {
      fetch(tourId) flatMap {
        _ ?? run
      }
    }

  private object publish {
    private val debouncer = system.actorOf(
      Props(
        new Debouncer(
          15 seconds,
          { (_: Debouncer.Nothing) =>
            implicit val lang = lila.i18n.defaultLang
            fetchUpdateTournaments flatMap apiJsonView.apply foreach { json =>
              Bus.publish(
                SendToFlag("tournament", Json.obj("t" -> "reload", "d" -> json)),
                "sendToFlag"
              )
            }
            cached.onHomepage.get {} foreach { tours =>
              renderer.actor ? Tournament.TournamentTable(tours) map { case view: String =>
                Bus.publish(ReloadTournaments(view), "lobbySocket")
              }
            }
          }
        )
      )
    )
    def apply(): Unit = { debouncer ! Debouncer.Nothing }
  }

  private object updateTournamentStanding {

    import lila.hub.EarlyMultiThrottler

    // last published top hashCode
    private val lastPublished = lila.memo.CacheApi.scaffeineNoScheduler
      .initialCapacity(16)
      .expireAfterWrite(2 minute)
      .build[Tournament.ID, Int]()

    private def publishNow(tourId: Tournament.ID) =
      tournamentTop(tourId) map { top =>
        val lastHash: Int = ~lastPublished.getIfPresent(tourId)
        if (lastHash != top.hashCode) {
          Bus.publish(
            lila.hub.actorApi.round.TourStanding(tourId, JsonView.top(top, lightUserApi.sync)),
            "tourStanding"
          )
          lastPublished.put(tourId, top.hashCode)
        }
      }

    private val throttler = system.actorOf(Props(new EarlyMultiThrottler(logger = logger)))

    def apply(tour: Tournament): Unit =
      if (!tour.isTeamBattle)
        throttler ! EarlyMultiThrottler.Work(
          id = tour.id,
          run = () => publishNow(tour.id),
          cooldown = 15.seconds
        )
  }
}

private object TournamentApi {

  case class Callbacks(
      clearJsonViewCache: Tournament => Unit,
      clearWinnersCache: Tournament => Unit,
      clearTrophyCache: Tournament => Unit,
      indexLeaderboard: Tournament => Funit
  )
}
