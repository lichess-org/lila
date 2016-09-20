package lila.tournament

import akka.actor.{ Props, ActorRef, ActorSelection, ActorSystem }
import akka.pattern.{ ask, pipe }
import chess.{ Mode, StartingPosition }
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._
import scalaz.NonEmptyList

import actorApi._
import lila.common.Debouncer
import lila.db.dsl._
import lila.game.{ Game, GameRepo, Pov }
import lila.hub.actorApi.lobby.ReloadTournaments
import lila.hub.actorApi.map.{ Tell, TellIds }
import lila.hub.actorApi.timeline.{ Propagate, TourJoin }
import lila.hub.Sequencer
import lila.round.actorApi.round.{ GoBerserk, TournamentStanding, AbortForce }
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short

final class TournamentApi(
    cached: Cached,
    scheduleJsonView: ScheduleJsonView,
    system: ActorSystem,
    sequencers: ActorRef,
    autoPairing: AutoPairing,
    clearJsonViewCache: String => Funit,
    renderer: ActorSelection,
    timeline: ActorSelection,
    socketHub: ActorRef,
    site: ActorSelection,
    lobby: ActorSelection,
    roundMap: ActorRef,
    trophyApi: lila.user.TrophyApi,
    verify: Condition.Verify,
    indexLeaderboard: Tournament => Funit,
    roundSocketHub: ActorSelection) {

  def createTournament(setup: TournamentSetup, me: User): Fu[Tournament] = {
    var variant = chess.variant.Variant orDefault setup.variant
    val tour = Tournament.make(
      createdByUserId = me.id,
      clock = TournamentClock((setup.clockTime * 60).toInt, setup.clockIncrement),
      minutes = setup.minutes,
      waitMinutes = setup.waitMinutes,
      mode = setup.mode.fold(Mode.default)(Mode.orDefault),
      `private` = setup.isPrivate,
      password = setup.password.ifTrue(setup.isPrivate),
      system = System.Arena,
      variant = variant,
      position = StartingPosition.byEco(setup.position).ifTrue(variant.standard) | StartingPosition.initial)
    logger.info(s"Create $tour")
    TournamentRepo.insert(tour) >>- join(tour.id, me, tour.password) inject tour
  }

  private[tournament] def createScheduled(schedule: Schedule): Funit =
    (Schedule durationFor schedule) ?? { minutes =>
      val created = Tournament.schedule(schedule, minutes)
      logger.info(s"Create $created")
      TournamentRepo.insert(created).void >>- publish()
    }

  def makePairings(oldTour: Tournament, users: WaitingUsers, startAt: Long) {
    Sequencing(oldTour.id)(TournamentRepo.startedById) { tour =>
      cached ranking tour flatMap { ranking =>
        tour.system.pairingSystem.createPairings(tour, users, ranking).flatMap {
          case Nil => funit
          case pairings if nowMillis - startAt > 1200 =>
            pairingLogger.warn(s"Give up making https://lichess.org/tournament/${tour.id} ${pairings.size} pairings in ${nowMillis - startAt}ms")
            lila.mon.tournament.pairing.giveup()
            funit
          case pairings => pairings.map { pairing =>
            PairingRepo.insert(pairing) >>
              autoPairing(tour, pairing) addEffect { game =>
                sendTo(tour.id, StartGame(game))
              }
          }.sequenceFu >> featureOneOf(tour, pairings, ranking) >>- {
            lila.mon.tournament.pairing.create(pairings.size)
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

  def start(oldTour: Tournament) {
    Sequencing(oldTour.id)(TournamentRepo.createdById) { tour =>
      TournamentRepo.setStatus(tour.id, Status.Started) >>-
        sendTo(tour.id, Reload) >>-
        publish()
    }
  }

  def wipe(tour: Tournament): Funit =
    TournamentRepo.remove(tour).void >>
      PairingRepo.removeByTour(tour.id) >>
      PlayerRepo.removeByTour(tour.id) >>- publish() >>- socketReload(tour.id)

  def finish(oldTour: Tournament) {
    Sequencing(oldTour.id)(TournamentRepo.startedById) { tour =>
      PairingRepo count tour.id flatMap {
        case 0 => wipe(tour)
        case _ => for {
          _ <- TournamentRepo.setStatus(tour.id, Status.Finished)
          _ <- PlayerRepo unWithdraw tour.id
          _ <- PairingRepo removePlaying tour.id
          winner <- PlayerRepo winner tour.id
          _ <- winner.??(p => TournamentRepo.setWinnerId(tour.id, p.userId))
          _ <- clearJsonViewCache(tour.id)
        } yield {
          sendTo(tour.id, Reload)
          publish()
          PlayerRepo withPoints tour.id foreach {
            _ foreach { p => UserRepo.incToints(p.userId, p.score) }
          }
          awardTrophies(tour)
          indexLeaderboard(tour)
        }
      }
    }
  }

  private def awardTrophies(tour: Tournament): Funit =
    tour.schedule.??(_.freq == Schedule.Freq.Marathon) ?? {
      PlayerRepo.bestByTourWithRank(tour.id, 100).flatMap {
        _.map {
          case rp if rp.rank == 1  => trophyApi.award(rp.player.userId, _.MarathonWinner)
          case rp if rp.rank <= 10 => trophyApi.award(rp.player.userId, _.MarathonTopTen)
          case rp if rp.rank <= 50 => trophyApi.award(rp.player.userId, _.MarathonTopFifty)
          case rp                  => trophyApi.award(rp.player.userId, _.MarathonTopHundred)
        }.sequenceFu.void
      }
    }

  def verdicts(tour: Tournament, me: Option[User]): Fu[Condition.All.WithVerdicts] = me match {
    case None       => fuccess(tour.conditions.accepted)
    case Some(user) => verify(tour.conditions, user)
  }

  def join(tourId: String, me: User, p: Option[String]) {
    Sequencing(tourId)(TournamentRepo.enterableById) { tour =>
      if (tour.password == p) {
        verdicts(tour, me.some) flatMap {
          _.accepted ?? {
            PlayerRepo.join(tour.id, me, tour.perfLens) >> updateNbPlayers(tour.id) >>- {
              withdrawOtherTournaments(tour.id, me.id)
              socketReload(tour.id)
              publish()
              if (!tour.`private`) timeline ! {
                Propagate(TourJoin(me.id, tour.id, tour.fullName)) toFollowersOf me.id
              }
            }
          }
        }
      } else fuccess(socketReload(tour.id))
    }
  }

  private def updateNbPlayers(tourId: String) =
    PlayerRepo count tourId flatMap { TournamentRepo.setNbPlayers(tourId, _) }

  private def withdrawOtherTournaments(tourId: String, userId: String) {
    TournamentRepo toursToWithdrawWhenEntering tourId foreach {
      _ foreach { other =>
        PlayerRepo.exists(other.id, userId) foreach {
          _ ?? withdraw(other.id, userId)
        }
      }
    }
  }

  def withdraw(tourId: String, userId: String) {
    Sequencing(tourId)(TournamentRepo.enterableById) {
      case tour if tour.isCreated =>
        PlayerRepo.remove(tour.id, userId) >> updateNbPlayers(tour.id) >>- socketReload(tour.id) >>- publish()
      case tour if tour.isStarted =>
        PlayerRepo.withdraw(tour.id, userId) >>- socketReload(tour.id) >>- publish()
      case _ => funit
    }
  }

  def withdrawAll(user: User) {
    TournamentRepo.nonEmptyEnterable foreach {
      _ foreach { tour =>
        PlayerRepo.exists(tour.id, user.id) foreach {
          _ ?? withdraw(tour.id, user.id)
        }
      }
    }
  }

  def berserk(gameId: String, userId: String) {
    GameRepo game gameId foreach {
      _.flatMap(_.tournamentId) foreach { tourId =>
        Sequencing(tourId)(TournamentRepo.startedById) { tour =>
          PairingRepo.findPlaying(tour.id, userId) flatMap {
            case Some(pairing) if pairing.berserkOf(userId) == 0 =>
              (pairing povRef userId) ?? { povRef =>
                GameRepo pov povRef flatMap {
                  _.filter(_.game.berserkable) ?? { pov =>
                    PairingRepo.setBerserk(pairing, userId, 1) >>- {
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
  }

  def finishGame(game: Game) {
    game.tournamentId foreach { tourId =>
      Sequencing(tourId)(TournamentRepo.startedById) { tour =>
        PairingRepo.finish(game) >>
          game.userIds.map(updatePlayer(tour)).sequenceFu.void >>-
          socketReload(tour.id) >>- updateTournamentStanding(tour)
      }
    }
  }

  private def updatePlayer(tour: Tournament)(userId: String): Funit =
    (tour.perfType.ifTrue(tour.mode.rated) ?? { UserRepo.perfOf(userId, _) }) flatMap { perf =>
      PlayerRepo.update(tour.id, userId) { player =>
        PairingRepo.finishedByPlayerChronological(tour.id, userId) map { pairings =>
          val sheet = tour.system.scoringSystem.sheet(tour, userId, pairings)
          player.copy(
            score = sheet.total,
            fire = sheet.onFire,
            ratingDiff = perf.fold(player.ratingDiff)(_.intRating - player.rating),
            provisional = perf.fold(player.provisional)(_.provisional)
          ).recomputeMagicScore
        }
      }
    }

  def ejectLame(userId: String) {
    TournamentRepo.recentAndNext foreach {
      _ foreach { tour =>
        PlayerRepo.exists(tour.id, userId) foreach {
          _ ?? ejectLame(tour.id, userId)
        }
      }
    }
  }

  def ejectLame(tourId: String, userId: String) {
    Sequencing(tourId)(TournamentRepo.byId) { tour =>
      PlayerRepo.remove(tour.id, userId) >> {
        if (tour.isStarted)
          PairingRepo.findPlaying(tour.id, userId).map {
            _ foreach { currentPairing =>
              roundMap ! Tell(currentPairing.gameId, AbortForce)
            }
          } >> PairingRepo.opponentsOf(tour.id, userId).flatMap { uids =>
            PairingRepo.removeByTourAndUserId(tour.id, userId) >>
              lila.common.Future.applySequentially(uids.toList)(updatePlayer(tour))
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
  }

  private val miniStandingCache = lila.memo.AsyncCache[String, List[RankedPlayer]](
    (id: String) => PlayerRepo.bestByTourWithRank(id, 30),
    timeToLive = 3 second)

  def miniStanding(tourId: String, withStanding: Boolean): Fu[Option[MiniStanding]] =
    TournamentRepo byId tourId flatMap {
      _ ?? { tour =>
        if (withStanding) miniStandingCache(tour.id) map { rps =>
          MiniStanding(tour, rps.some).some
        }
        else fuccess(MiniStanding(tour, none).some)
      }
    }

  def miniStanding(tourId: String, userId: Option[String], withStanding: Boolean): Fu[Option[MiniStanding]] =
    userId ?? { uid =>
      PlayerRepo.exists(tourId, uid) flatMap {
        _ ?? miniStanding(tourId, withStanding)
      }
    }

  def fetchVisibleTournaments: Fu[VisibleTournaments] =
    TournamentRepo.publicCreatedSorted(6 * 60) zip
      TournamentRepo.publicStarted zip
      TournamentRepo.finishedNotable(20) map {
        case ((created, started), finished) =>
          VisibleTournaments(created, started, finished)
      }

  def playerInfo(tourId: String, userId: String): Fu[Option[PlayerInfoExt]] =
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

  private def fetchGames(tour: Tournament, ids: Seq[String]) =
    if (tour.isFinished) GameRepo gamesFromSecondary ids
    else GameRepo gamesFromPrimary ids

  private def playerPovs(tour: Tournament, userId: String, nb: Int): Fu[List[Pov]] =
    PairingRepo.recentIdsByTourAndUserId(tour.id, userId, nb) flatMap { ids =>
      fetchGames(tour, ids) map {
        _.flatMap { Pov.ofUserId(_, userId) }
      }
    }

  private def sequence(tourId: String)(work: => Funit) {
    sequencers ! Tell(tourId, Sequencer work work)
  }

  private def Sequencing(tourId: String)(fetch: String => Fu[Option[Tournament]])(run: Tournament => Funit) {
    sequence(tourId) {
      fetch(tourId) flatMap {
        case Some(t) => run(t)
        case None    => fufail(s"Can't run sequenced operation on missing tournament $tourId")
      }
    }
  }

  private def socketReload(tourId: String) {
    sendTo(tourId, Reload)
  }

  private object publish {
    private val debouncer = system.actorOf(Props(new Debouncer(10 seconds, {
      (_: Debouncer.Nothing) =>
        fetchVisibleTournaments foreach { vis =>
          site ! SendToFlag("tournament", Json.obj(
            "t" -> "reload",
            "d" -> scheduleJsonView(vis)
          ))
        }
        TournamentRepo.promotable foreach { tours =>
          renderer ? TournamentTable(tours) map {
            case view: play.twirl.api.Html => ReloadTournaments(view.body)
          } pipeToSelection lobby
        }
    })))
    def apply() { debouncer ! Debouncer.Nothing }
  }

  private object updateTournamentStanding {
    private val debouncer = system.actorOf(Props(new Debouncer(10 seconds, {
      (tourId: String) =>
        PairingRepo playingGameIds tourId foreach { ids =>
          roundSocketHub ! TellIds(ids, TournamentStanding(tourId))
        }
    })))
    def apply(tour: Tournament) {
      debouncer ! tour.id
    }
  }

  private def sendTo(tourId: String, msg: Any) {
    socketHub ! Tell(tourId, msg)
  }
}
