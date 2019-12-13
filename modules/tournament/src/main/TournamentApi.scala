package lila.tournament

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.stream.scaladsl._
import com.github.ghik.silencer.silent
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.common.config.MaxPerSecond
import lila.common.{ Bus, Debouncer, LightUser, WorkQueues }
import lila.game.{ Game, GameRepo, LightPov }
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
    trophyApi: lila.user.TrophyApi,
    verify: Condition.Verify,
    duelStore: DuelStore,
    pause: Pause,
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi,
    proxyRepo: lila.round.GameProxyRepo
)(implicit system: ActorSystem, mat: akka.stream.Materializer) {

  private val workQueue = new WorkQueues(256, 1 minute)

  def createTournament(
      setup: TournamentSetup,
      me: User,
      myTeams: List[LightTeam],
      getUserTeamIds: User => Fu[List[TeamID]]
  ): Fu[Tournament] = {
    val tour = Tournament.make(
      by = Right(me),
      name = DataForm.canPickName(me) ?? setup.name,
      clock = setup.clockConfig,
      minutes = setup.minutes,
      waitMinutes = setup.waitMinutes | DataForm.waitMinuteDefault,
      startDate = setup.startDate,
      mode = setup.realMode,
      password = setup.password,
      variant = setup.realVariant,
      position =
        DataForm.startingPosition(setup.position | chess.StartingPosition.initial.fen, setup.realVariant),
      berserkable = setup.berserkable | true,
      teamBattle = setup.teamBattleByTeam map TeamBattle.init
    ) |> { tour =>
      tour.perfType.fold(tour) { perfType =>
        tour.copy(conditions = setup.conditions.convert(perfType, myTeams.view.map(_.pair).toMap))
      }
    }
    if (tour.name != me.titleUsername && lila.common.LameName.anyNameButLichessIsOk(tour.name))
      Bus.publish(lila.hub.actorApi.slack.TournamentName(me.username, tour.id, tour.name), "slack")
    tournamentRepo.insert(tour) >>
      join(tour.id, me, tour.password, setup.teamBattleByTeam, getUserTeamIds, none) inject tour
  }

  private[tournament] def create(tournament: Tournament): Funit = {
    tournamentRepo.insert(tournament).void
  }

  def teamBattleUpdate(
      tour: Tournament,
      data: TeamBattle.DataForm.Setup,
      filterExistingTeamIds: Set[TeamID] => Fu[Set[TeamID]]
  ): Funit =
    filterExistingTeamIds(data.potentialTeamIds) flatMap { teamIds =>
      tournamentRepo.setTeamBattle(tour.id, TeamBattle(teamIds, data.nbLeaders))
    }

  private[tournament] def makePairings(oldTour: Tournament, users: WaitingUsers): Funit =
    Sequencing(oldTour.id)(tournamentRepo.startedById) { tour =>
      cached
        .ranking(tour)
        .flatMap { ranking =>
          pairingSystem.createPairings(tour, users, ranking).flatMap {
            case Nil => funit
            case pairings =>
              userRepo.idsMap(pairings.flatMap(_.users)) flatMap { users =>
                pairings.map { pairing =>
                  pairingRepo.insert(pairing) >>
                    autoPairing(tour, pairing, users, ranking) map {
                    socket.startGame(tour.id, _)
                  }
                }.sequenceFu >>
                  featureOneOf(tour, pairings, ranking) >>-
                  lila.mon.tournament.pairing.count.increment(pairings.size)
              }
          }
        }
        .monSuccess(_.tournament.pairing.create)
        .chronometer
        .logIfSlow(100, logger)(_ => s"Pairings for https://lichess.org/tournament/${tour.id}")
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

  def tourAndRanks(game: Game): Fu[Option[TourAndRanks]] = ~ {
    for {
      tourId  <- game.tournamentId
      whiteId <- game.whitePlayer.userId
      blackId <- game.blackPlayer.userId
    } yield tournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        cached ranking tour map { ranking =>
          ranking.get(whiteId) |@| ranking.get(blackId) apply {
            case (whiteR, blackR) => TourAndRanks(tour, whiteR + 1, blackR + 1)
          }
        }
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
          }
      }
    }

  def kill(tour: Tournament): Funit =
    if (tour.isStarted) finish(tour)
    else if (tour.isCreated) destroy(tour)
    else funit

  private def awardTrophies(tour: Tournament): Funit = {
    import lila.user.TrophyKind._
    tour.schedule.??(_.freq == Schedule.Freq.Marathon) ?? {
      playerRepo.bestByTourWithRank(tour.id, 100).flatMap {
        _.map {
          case rp if rp.rank == 1  => trophyApi.award(rp.player.userId, marathonWinner)
          case rp if rp.rank <= 10 => trophyApi.award(rp.player.userId, marathonTopTen)
          case rp if rp.rank <= 50 => trophyApi.award(rp.player.userId, marathonTopFifty)
          case rp                  => trophyApi.award(rp.player.userId, marathonTopHundred)
        }.sequenceFu.void
      }
    }
  }

  def verdicts(
      tour: Tournament,
      me: Option[User],
      getUserTeamIds: User => Fu[List[TeamID]]
  ): Fu[Condition.All.WithVerdicts] = me match {
    case None       => fuccess(tour.conditions.accepted)
    case Some(user) => verify(tour.conditions, user, getUserTeamIds)
  }

  private[tournament] def join(
      tourId: Tournament.ID,
      me: User,
      password: Option[String],
      withTeamId: Option[String],
      getUserTeamIds: User => Fu[List[TeamID]],
      promise: Option[Promise[Boolean]]
  ): Funit = Sequencing(tourId)(tournamentRepo.enterableById) { tour =>
    val fuJoined =
      if (tour.password == password) {
        verdicts(tour, me.some, getUserTeamIds) flatMap {
          _.accepted ?? {
            pause.canJoin(me.id, tour) ?? {
              def proceedWithTeam(team: Option[String]) =
                tournamentRepo.tourIdsToWithdrawWhenEntering(tourId).flatMap(withdrawMany(_, me.id)) >>
                  playerRepo.join(tour.id, me, tour.perfLens, team) >>
                  updateNbPlayers(tour.id) >>- {
                  socket.reload(tour.id)
                  publish()
                } inject true
              withTeamId match {
                case None if !tour.isTeamBattle => proceedWithTeam(none)
                case None if tour.isTeamBattle =>
                  playerRepo.exists(tour.id, me.id) flatMap {
                    case true  => proceedWithTeam(none)
                    case false => fuccess(false)
                  }
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
      promise.foreach(_ success joined)
    }
  }

  def joinWithResult(
      tourId: Tournament.ID,
      me: User,
      password: Option[String],
      teamId: Option[String],
      getUserTeamIds: User => Fu[List[TeamID]]
  ): Fu[Boolean] = {
    val promise = Promise[Boolean]
    join(tourId, me, password, teamId, getUserTeamIds, promise.some)
    promise.future.withTimeoutDefault(5.seconds, false)(system)
  }

  def pageOf(tour: Tournament, userId: User.ID): Fu[Option[Int]] =
    cached ranking tour map {
      _ get userId map { rank =>
        (Math.floor(rank / 10) + 1).toInt
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
        playerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id) >>- socket.reload(tour.id) >>- publish()
      case tour if tour.isStarted =>
        for {
          _ <- playerRepo.withdraw(tour.id, userId)
          pausable <- if (isPause) cached.ranking(tour).map { _ get userId exists (7 >) } else
            fuccess(isStalling)
        } yield {
          if (pausable) pause.add(userId)
          socket.reload(tour.id)
          publish()
        }
      case _ => funit
    }

  private def withdrawMany(tourIds: List[Tournament.ID], userId: User.ID): Funit =
    playerRepo.filterExists(tourIds, userId) flatMap {
      _.map {
        withdraw(_, userId, isPause = false, isStalling = false)
      }.sequenceFu.void
    }

  def withdrawAll(user: User): Funit =
    tournamentRepo.nonEmptyEnterableIds flatMap { withdrawMany(_, user.id) }

  private[tournament] def berserk(gameId: Game.ID, userId: User.ID): Funit =
    proxyRepo game gameId flatMap {
      _.filter(_.berserkable) ?? { game =>
        game.tournamentId ?? { tourId =>
          Sequencing(tourId)(tournamentRepo.startedById) { tour =>
            pairingRepo.findPlaying(tour.id, userId) flatMap {
              case Some(pairing) if !pairing.berserkOf(userId) =>
                (pairing colorOf userId) ?? { color =>
                  pairingRepo.setBerserk(pairing, userId) >>-
                    tellRound(gameId, GoBerserk(color))
                }
              case _ => funit
            }
          }
        }
      }
    }

  private[tournament] def finishGame(game: Game): Funit = game.tournamentId ?? { tourId =>
    Sequencing(tourId)(tournamentRepo.startedById) { tour =>
      pairingRepo.finish(game) >>
        game.userIds.map(updatePlayer(tour, game.some)).sequenceFu.void >>- {
        duelStore.remove(game)
        socket.reload(tour.id)
        updateTournamentStanding(tour.id)
        withdrawNonMover(game)
      }
    }
  }

  private[tournament] def sittingDetected(game: Game, player: User.ID): Funit =
    game.tournamentId ?? { stallPause(_, player) }

  private def updatePlayer(
      tour: Tournament,
      finishing: Option[Game] // if set, update the player performance. Leave to none to just recompute the sheet.
  )(userId: User.ID): Funit =
    (tour.perfType.ifTrue(tour.mode.rated) ?? { userRepo.perfOf(userId, _) }) flatMap { perf =>
      playerRepo.update(tour.id, userId) { player =>
        cached.sheet.update(tour, userId) map { sheet =>
          player.copy(
            score = sheet.total,
            fire = sheet.onFire,
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

  @silent private def withdrawNonMover(game: Game): Unit =
    for {
      tourId <- game.tournamentId
      if game.status == chess.Status.NoStart
      player <- game.playerWhoDidNotMove
      userId <- player.userId
    } withdraw(tourId, userId, isPause = false, isStalling = false)

  def pausePlaybanned(userId: User.ID) =
    tournamentRepo.startedIds flatMap {
      playerRepo.filterExists(_, userId) flatMap {
        _.map { tourId =>
          playerRepo.withdraw(tourId, userId) >>- socket.reload(tourId) >>- publish()
        }.sequenceFu.void
      }
    }

  def ejectLame(userId: User.ID, playedIds: List[Tournament.ID]): Funit =
    tournamentRepo.nonEmptyEnterableIds flatMap {
      playerRepo.filterExists(_, userId) flatMap { enteredIds =>
        (enteredIds ++ playedIds)
          .map { ejectLame(_, userId) }
          .sequenceFu
          .void
      }
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
          } else if (tour.isFinished && tour.winnerId.contains(userId))
          playerRepo winner tour.id flatMap {
            _ ?? { p =>
              tournamentRepo.setWinnerId(tour.id, p.userId)
            }
          } else funit
      } >>
        updateNbPlayers(tour.id) >>-
        socket.reload(tour.id) >>- publish()
    }

  private val tournamentTopCache = asyncCache.multi[Tournament.ID, TournamentTop](
    name = "tournament.top",
    id => playerRepo.bestByTour(id, 20) map TournamentTop.apply,
    expireAfter = _.ExpireAfterWrite(3 second)
  )

  def tournamentTop(tourId: Tournament.ID): Fu[TournamentTop] =
    tournamentTopCache get tourId

  def miniView(tourId: Tournament.ID, withTop: Boolean): Fu[Option[TourMiniView]] =
    tournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        withTop ?? { tournamentTop(tour.id) map some } map { TourMiniView(tour, _).some }
      }
    }

  def fetchVisibleTournaments: Fu[VisibleTournaments] =
    tournamentRepo.publicCreatedSorted(6 * 60) zip
      tournamentRepo.publicStarted zip
      tournamentRepo.finishedNotable(30) map {
      case ((created, started), finished) =>
        VisibleTournaments(created, started, finished)
    }

  def playerInfo(tour: Tournament, userId: User.ID): Fu[Option[PlayerInfoExt]] =
    userRepo named userId flatMap {
      _ ?? { user =>
        playerRepo.find(tour.id, user.id) flatMap {
          _ ?? { player =>
            playerPovs(tour, user.id, 50) map { povs =>
              PlayerInfoExt(user, player, povs).some
            }
          }
        }
      }
    }

  def allCurrentLeadersInStandard: Fu[Map[Tournament, TournamentTop]] =
    tournamentRepo.standardPublicStartedFromSecondary.flatMap { tours =>
      tours
        .map { tour =>
          tournamentTop(tour.id) map (tour -> _)
        }
        .sequenceFu
        .map(_.toMap)
    }

  def calendar: Fu[List[Tournament]] = {
    val from = DateTime.now.minusDays(1)
    tournamentRepo.calendar(from = from, to = from plusYears 1)
  }

  def resultStream(tour: Tournament, perSecond: MaxPerSecond, nb: Int): Source[Player.Result, _] =
    playerRepo
      .sortedCursor(tour.id, perSecond.value)
      .documentSource()
      .take(nb)
      .throttle(perSecond.value, 1 second)
      .zipWithIndex
      .mapAsync(8) {
        case (player, index) =>
          lightUserApi.async(player.userId) map { lu =>
            Player.Result(player, lu | LightUser.fallback(player.userId), index.toInt + 1)
          }
      }

  def byOwnerStream(owner: User, perSecond: MaxPerSecond, nb: Int): Source[Tournament, _] =
    tournamentRepo
      .sortedCursor(owner, perSecond.value)
      .documentSource()
      .take(nb)
      .throttle(perSecond.value, 1 second)

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
        case Some(t) => run(t)
        case None    => fufail(s"Can't run sequenced operation on missing tournament $tourId")
      }
    }

  private object publish {
    private val debouncer = system.actorOf(Props(new Debouncer(15 seconds, { (_: Debouncer.Nothing) =>
      fetchVisibleTournaments flatMap apiJsonView.apply foreach { json =>
        Bus.publish(
          SendToFlag("tournament", Json.obj("t" -> "reload", "d" -> json)),
          "sendToFlag"
        )
      }
      tournamentRepo.promotable foreach { tours =>
        renderer.actor ? Tournament.TournamentTable(tours) map {
          case view: String => Bus.publish(ReloadTournaments(view), "lobbySocket")
        }
      }
    })))
    def apply(): Unit = { debouncer ! Debouncer.Nothing }
  }

  private object updateTournamentStanding {

    import lila.hub.EarlyMultiThrottler
    import com.github.blemale.scaffeine.{ Cache, Scaffeine }

    // last published top hashCode
    private val lastPublished: Cache[Tournament.ID, Int] = Scaffeine()
      .expireAfterWrite(2 minute)
      .build[Tournament.ID, Int]

    private def publishNow(tourId: Tournament.ID) = tournamentTop(tourId) map { top =>
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

    def apply(tourId: Tournament.ID): Unit =
      throttler ! EarlyMultiThrottler.work(
        id = tourId,
        run = publishNow(tourId),
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
