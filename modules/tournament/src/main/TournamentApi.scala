package lila.tournament

import akka.actor.{ Props, ActorRef, ActorSelection, ActorSystem }
import akka.pattern.{ ask, pipe }
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.Debouncer
import lila.game.{ Game, LightGame, GameRepo, Pov, LightPov }
import lila.hub.actorApi.lobby.ReloadTournaments
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.timeline.{ Propagate, TourJoin }
import lila.hub.Sequencer
import lila.round.actorApi.round.{ GoBerserk, AbortForce }
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short

final class TournamentApi(
    cached: Cached,
    scheduleJsonView: ScheduleJsonView,
    system: ActorSystem,
    sequencers: ActorRef,
    autoPairing: AutoPairing,
    clearJsonViewCache: Tournament.ID => Unit,
    clearWinnersCache: Tournament => Unit,
    clearTrophyCache: Tournament => Unit,
    renderer: ActorSelection,
    timeline: ActorSelection,
    socketHub: ActorRef,
    site: ActorSelection,
    lobby: ActorSelection,
    roundMap: ActorRef,
    trophyApi: lila.user.TrophyApi,
    verify: Condition.Verify,
    indexLeaderboard: Tournament => Funit,
    duelStore: DuelStore,
    pause: Pause,
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi
) {

  private val bus = system.lilaBus

  def createTournament(setup: TournamentSetup, me: User): Fu[Tournament] = {
    val tour = Tournament.make(
      by = Right(me),
      name = DataForm.canPickName(me) ?? setup.name,
      clock = setup.clockConfig,
      minutes = setup.minutes,
      waitMinutes = setup.waitMinutes,
      mode = setup.realMode,
      `private` = setup.isPrivate,
      password = setup.password.ifTrue(setup.isPrivate),
      system = System.Arena,
      variant = setup.realVariant,
      position = DataForm.startingPosition(setup.position, setup.realVariant),
      berserkable = setup.berserkable
    ) |> { tour =>
        tour.perfType.fold(tour) { perfType =>
          tour.copy(conditions = setup.conditions convert perfType)
        }
      }
    if (tour.name != me.titleUsername && lila.common.LameName.anyNameButLichessIsOk(tour.name)) {
      val msg = s"""@${me.username} created tournament "${tour.name} Arena" :kappa: https://lichess.org/tournament/${tour.id}"""
      logger warn msg
      bus.publish(lila.hub.actorApi.slack.Warning(msg), 'slack)
    }
    logger.info(s"Create $tour")
    TournamentRepo.insert(tour) >>- join(tour.id, me, tour.password) inject tour
  }

  private[tournament] def createFromPlan(plan: Schedule.Plan): Funit = {
    val minutes = Schedule durationFor plan.schedule
    val tournament = plan build Tournament.schedule(plan.schedule, minutes)
    logger.info(s"Create $tournament")
    TournamentRepo.insert(tournament).void
  }

  private[tournament] def makePairings(oldTour: Tournament, users: WaitingUsers, startAt: Long): Unit = {
    Sequencing(oldTour.id)(TournamentRepo.startedById) { tour =>
      cached ranking tour flatMap { ranking =>
        tour.system.pairingSystem.createPairings(tour, users, ranking).flatMap {
          case Nil => funit
          case pairings if nowMillis - startAt > 1200 =>
            pairingLogger.warn(s"Give up making https://lichess.org/tournament/${tour.id} ${pairings.size} pairings in ${nowMillis - startAt}ms")
            lila.mon.tournament.pairing.giveup()
            funit
          case pairings => UserRepo.idsMap(pairings.flatMap(_.users)) flatMap { users =>
            pairings.map { pairing =>
              PairingRepo.insert(pairing) >>
                autoPairing(tour, pairing, users, ranking) addEffect { game =>
                  sendTo(tour.id, StartGame(game))
                }
            }.sequenceFu >> featureOneOf(tour, pairings, ranking) >>- {
              lila.mon.tournament.pairing.create(pairings.size)
            }
          }
        } >>- {
          val time = nowMillis - startAt
          lila.mon.tournament.pairing.createTime(time.toInt)
          if (time > 100)
            pairingLogger.debug(s"Done making https://lichess.org/tournament/${tour.id} in ${time}ms")
        }
      }
    }
  }

  private def featureOneOf(tour: Tournament, pairings: Pairings, ranking: Ranking): Funit =
    tour.featuredId.ifTrue(pairings.nonEmpty) ?? PairingRepo.byId map2
      RankedPairing(ranking) map (_.flatten) flatMap { curOption =>
        pairings.flatMap(RankedPairing(ranking)).sortBy(_.bestRank).headOption ?? { bestCandidate =>
          def switch = TournamentRepo.setFeaturedGameId(tour.id, bestCandidate.pairing.gameId)
          curOption.filter(_.pairing.playing) match {
            case Some(current) if bestCandidate.bestRank < current.bestRank => switch
            case Some(_) => funit
            case _ => switch
          }
        }
      }

  def tourAndRanks(game: Game): Fu[Option[TourAndRanks]] = ~{
    for {
      tourId <- game.tournamentId
      whiteId <- game.whitePlayer.userId
      blackId <- game.blackPlayer.userId
    } yield TournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        cached ranking tour map { ranking =>
          ranking.get(whiteId) |@| ranking.get(blackId) apply {
            case (whiteR, blackR) => TourAndRanks(tour, whiteR + 1, blackR + 1)
          }
        }
      }
    }
  }

  def start(oldTour: Tournament): Unit =
    Sequencing(oldTour.id)(TournamentRepo.createdById) { tour =>
      TournamentRepo.setStatus(tour.id, Status.Started) >>-
        sendTo(tour.id, Reload) >>-
        publish()
    }

  def wipe(tour: Tournament): Funit =
    TournamentRepo.remove(tour).void >>
      PairingRepo.removeByTour(tour.id) >>
      PlayerRepo.removeByTour(tour.id) >>- publish() >>- socketReload(tour.id)

  def finish(oldTour: Tournament): Unit = {
    Sequencing(oldTour.id)(TournamentRepo.startedById) { tour =>
      PairingRepo count tour.id flatMap {
        case 0 => wipe(tour)
        case _ => for {
          _ <- TournamentRepo.setStatus(tour.id, Status.Finished)
          _ <- PlayerRepo unWithdraw tour.id
          _ <- PairingRepo removePlaying tour.id
          winner <- PlayerRepo winner tour.id
          _ <- winner.??(p => TournamentRepo.setWinnerId(tour.id, p.userId))
        } yield {
          clearJsonViewCache(tour.id)
          sendTo(tour.id, Reload)
          publish()
          PlayerRepo withPoints tour.id foreach {
            _ foreach { p => UserRepo.incToints(p.userId, p.score) }
          }
          awardTrophies(tour).logFailure(logger, _ => s"${tour.id} awardTrophies")
          indexLeaderboard(tour).logFailure(logger, _ => s"${tour.id} indexLeaderboard")
          clearWinnersCache(tour)
          clearTrophyCache(tour)
        }
      }
    }
  }

  def kill(tour: Tournament): Unit = {
    if (tour.isStarted) finish(tour)
    else if (tour.isCreated) wipe(tour)
  }

  private def awardTrophies(tour: Tournament): Funit =
    tour.schedule.??(_.freq == Schedule.Freq.Marathon) ?? {
      PlayerRepo.bestByTourWithRank(tour.id, 100).flatMap {
        _.map {
          case rp if rp.rank == 1 => trophyApi.award(rp.player.userId, _.MarathonWinner)
          case rp if rp.rank <= 10 => trophyApi.award(rp.player.userId, _.MarathonTopTen)
          case rp if rp.rank <= 50 => trophyApi.award(rp.player.userId, _.MarathonTopFifty)
          case rp => trophyApi.award(rp.player.userId, _.MarathonTopHundred)
        }.sequenceFu.void
      }
    }

  def verdicts(tour: Tournament, me: Option[User]): Fu[Condition.All.WithVerdicts] = me match {
    case None => fuccess(tour.conditions.accepted)
    case Some(user) => verify(tour.conditions, user)
  }

  def join(tourId: Tournament.ID, me: User, p: Option[String]): Unit = {
    Sequencing(tourId)(TournamentRepo.enterableById) { tour =>
      if (tour.password == p) {
        verdicts(tour, me.some) flatMap {
          _.accepted ?? {
            pause.canJoin(me.id, tour) ?? {
              PlayerRepo.join(tour.id, me, tour.perfLens) >> updateNbPlayers(tour.id) >>- {
                withdrawOtherTournaments(tour.id, me.id)
                socketReload(tour.id)
                publish()
              }
            }
          }
        }
      } else fuccess(socketReload(tour.id))
    }
  }

  def joinWithResult(tourId: Tournament.ID, me: User, p: Option[String]): Fu[Boolean] = {
    join(tourId, me, p)
    // atrocious hack, because joining is fire and forget
    akka.pattern.after(500 millis, system.scheduler) {
      PlayerRepo.find(tourId, me.id) map { _ ?? (_.active) }
    }
  }

  def pageOf(tour: Tournament, userId: User.ID): Fu[Option[Int]] =
    cached ranking tour map {
      _ get userId map { rank =>
        (Math.floor(rank / 10) + 1).toInt
      }
    }

  private def updateNbPlayers(tourId: Tournament.ID) =
    PlayerRepo count tourId flatMap { TournamentRepo.setNbPlayers(tourId, _) }

  private def withdrawOtherTournaments(tourId: Tournament.ID, userId: User.ID): Unit =
    TournamentRepo tourIdsToWithdrawWhenEntering tourId foreach {
      PlayerRepo.filterExists(_, userId) foreach {
        _ foreach {
          withdraw(_, userId, isPause = false)
        }
      }
    }

  def selfPause(tourId: Tournament.ID, userId: User.ID): Unit =
    withdraw(tourId, userId, isPause = true)

  private def withdraw(tourId: Tournament.ID, userId: User.ID, isPause: Boolean): Unit = {
    Sequencing(tourId)(TournamentRepo.enterableById) {
      case tour if tour.isCreated =>
        PlayerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id) >>- socketReload(tour.id) >>- publish()
      case tour if tour.isStarted => for {
        _ <- PlayerRepo.withdraw(tour.id, userId)
        pausable <- isPause ?? cached.ranking(tour).map { _ get userId exists (7>) }
      } yield {
        if (pausable) pause.add(userId, tour)
        socketReload(tour.id)
        publish()
      }
      case _ => funit
    }
  }

  def withdrawAll(user: User): Unit =
    TournamentRepo.nonEmptyEnterableIds foreach {
      PlayerRepo.filterExists(_, user.id) foreach {
        _ foreach {
          withdraw(_, user.id, isPause = false)
        }
      }
    }

  def berserk(gameId: Game.ID, userId: User.ID): Unit =
    GameRepo tournamentId gameId foreach {
      _ foreach { tourId =>
        Sequencing(tourId)(TournamentRepo.startedById) { tour =>
          PairingRepo.findPlaying(tour.id, userId) flatMap {
            case Some(pairing) if !pairing.berserkOf(userId) =>
              (pairing povRef userId) ?? { povRef =>
                GameRepo pov povRef flatMap {
                  _.filter(_.game.berserkable) ?? { pov =>
                    PairingRepo.setBerserk(pairing, userId) >>- {
                      roundMap ! Tell(povRef.gameId, GoBerserk(povRef.color))
                    }
                  }
                }
              }
            case _ => funit
          }
        }
      }
    }

  def finishGame(game: Game): Unit =
    game.tournamentId foreach { tourId =>
      Sequencing(tourId)(TournamentRepo.startedById) { tour =>
        PairingRepo.finish(game) >>
          game.userIds.map(updatePlayer(tour, game.some)).sequenceFu.void >>- {
            duelStore.remove(game)
            socketReload(tour.id)
            updateTournamentStanding(tour.id)
            withdrawNonMover(game)
          }
      }
    }

  private def updatePlayer(
    tour: Tournament,
    finishing: Option[Game] // if set, update the player performance. Leave to none to just recompute the sheet.
  )(userId: User.ID): Funit =
    (tour.perfType.ifTrue(tour.mode.rated) ?? { UserRepo.perfOf(userId, _) }) flatMap { perf =>
      PlayerRepo.update(tour.id, userId) { player =>
        cached.sheet.update(tour, userId) map { sheet =>
          player.copy(
            score = sheet.total,
            fire = sheet.onFire,
            rating = perf.fold(player.rating)(_.intRating),
            provisional = perf.fold(player.provisional)(_.provisional),
            performance = {
              for {
                g <- finishing
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

  private def performanceOf(g: Game, userId: String): Option[Int] = for {
    opponent <- g.opponentByUserId(userId)
    opponentRating <- opponent.rating
    multiplier = g.winnerUserId.??(winner => if (winner == userId) 1 else -1)
  } yield opponentRating + 500 * multiplier

  private def withdrawNonMover(game: Game): Unit = for {
    tourId <- game.tournamentId
    if game.status == chess.Status.NoStart
    player <- game.playerWhoDidNotMove
    userId <- player.userId
  } withdraw(tourId, userId, isPause = false)

  def pausePlaybanned(userId: User.ID) =
    TournamentRepo.startedIds flatMap {
      PlayerRepo.filterExists(_, userId) flatMap {
        _.map { tourId =>
          PlayerRepo.withdraw(tourId, userId) >>- socketReload(tourId) >>- publish()
        }.sequenceFu.void
      }
    }

  def ejectLame(userId: User.ID, playedIds: List[Tournament.ID]): Unit =
    TournamentRepo.nonEmptyEnterableIds foreach {
      PlayerRepo.filterExists(_, userId) foreach { enteredIds =>
        (enteredIds ++ playedIds).foreach { ejectLame(_, userId) }
      }
    }

  def ejectLame(tourId: Tournament.ID, userId: User.ID): Unit =
    Sequencing(tourId)(TournamentRepo.byId) { tour =>
      PlayerRepo.remove(tour.id, userId) >> {
        if (tour.isStarted)
          PairingRepo.findPlaying(tour.id, userId).map {
            _ foreach { currentPairing =>
              roundMap ! Tell(currentPairing.gameId, AbortForce)
            }
          } >> PairingRepo.opponentsOf(tour.id, userId).flatMap { uids =>
            PairingRepo.removeByTourAndUserId(tour.id, userId) >>
              lila.common.Future.applySequentially(uids.toList)(updatePlayer(tour, none))
          }
        else if (tour.isFinished && tour.winnerId.contains(userId))
          PlayerRepo winner tour.id flatMap {
            _ ?? { p =>
              TournamentRepo.setWinnerId(tour.id, p.userId)
            }
          }
        else funit
      } >>
        updateNbPlayers(tour.id) >>-
        socketReload(tour.id) >>- publish()
    }

  private val tournamentTopCache = asyncCache.multi[Tournament.ID, TournamentTop](
    name = "tournament.top",
    id => PlayerRepo.bestByTour(id, 20) map TournamentTop.apply,
    expireAfter = _.ExpireAfterWrite(3 second)
  )

  def tournamentTop(tourId: Tournament.ID): Fu[TournamentTop] =
    tournamentTopCache get tourId

  def miniView(tourId: Tournament.ID, withTop: Boolean): Fu[Option[TourMiniView]] =
    TournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        withTop ?? { tournamentTop(tour.id) map some } map { TourMiniView(tour, _).some }
      }
    }

  def fetchVisibleTournaments: Fu[VisibleTournaments] =
    TournamentRepo.publicCreatedSorted(6 * 60) zip
      TournamentRepo.publicStarted zip
      TournamentRepo.finishedNotable(30) map {
        case ((created, started), finished) =>
          VisibleTournaments(created, started, finished)
      }

  def playerInfo(tourId: Tournament.ID, userId: User.ID): Fu[Option[PlayerInfoExt]] =
    UserRepo named userId flatMap {
      _ ?? { user =>
        TournamentRepo byId tourId flatMap {
          _ ?? { tour =>
            PlayerRepo.find(tour.id, user.id) flatMap {
              _ ?? { player =>
                playerPovs(tour, user.id, 50) map { povs =>
                  PlayerInfoExt(tour, user, player, povs).some
                }
              }
            }
          }
        }
      }
    }

  def allCurrentLeadersInStandard: Fu[Map[Tournament, TournamentTop]] =
    TournamentRepo.standardPublicStartedFromSecondary.flatMap { tours =>
      tours.map { tour =>
        tournamentTop(tour.id) map (tour -> _)
      }.sequenceFu.map(_.toMap)
    }

  def calendar: Fu[List[Tournament]] = {
    val from = DateTime.now.minusDays(1)
    TournamentRepo.calendar(from = from, to = from plusYears 1)
  }

  private def fetchGames(tour: Tournament, ids: Seq[Game.ID]): Fu[List[LightGame]] =
    GameRepo.light gamesFromPrimary ids

  private def playerPovs(tour: Tournament, userId: User.ID, nb: Int): Fu[List[LightPov]] =
    PairingRepo.recentIdsByTourAndUserId(tour.id, userId, nb) flatMap { ids =>
      fetchGames(tour, ids) map {
        _.flatMap { LightPov.ofUserId(_, userId) }
      }
    }

  private def sequence(tourId: Tournament.ID)(work: => Funit): Unit = {
    sequencers ! Tell(tourId, Sequencer work work)
  }

  private def Sequencing(tourId: Tournament.ID)(fetch: Tournament.ID => Fu[Option[Tournament]])(run: Tournament => Funit): Unit = {
    sequence(tourId) {
      fetch(tourId) flatMap {
        case Some(t) => run(t)
        case None => fufail(s"Can't run sequenced operation on missing tournament $tourId")
      }
    }
  }

  private def socketReload(tourId: Tournament.ID): Unit = sendTo(tourId, Reload)

  private object publish {
    private val debouncer = system.actorOf(Props(new Debouncer(15 seconds, {
      (_: Debouncer.Nothing) =>
        fetchVisibleTournaments flatMap scheduleJsonView.apply foreach { json =>
          site ! SendToFlag("tournament", Json.obj("t" -> "reload", "d" -> json))
        }
        TournamentRepo.promotable foreach { tours =>
          renderer ? TournamentTable(tours) map {
            case view: play.twirl.api.Html => ReloadTournaments(view.body)
          } pipeToSelection lobby
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
      if (lastHash != top.hashCode) bus.publish(
        lila.hub.actorApi.round.TourStanding(JsonView.top(top, lightUserApi.sync)),
        Symbol(s"tour-standing-$tourId")
      )
      lastPublished.put(tourId, top.hashCode)
    }

    private val throttler = system.actorOf(Props(new EarlyMultiThrottler(logger = logger)))

    def apply(tourId: Tournament.ID): Unit =
      throttler ! EarlyMultiThrottler.work(
        id = tourId,
        run = publishNow(tourId),
        cooldown = 15.seconds
      )
  }

  private def sendTo(tourId: Tournament.ID, msg: Any): Unit =
    socketHub ! Tell(tourId, msg)
}
