package lila.tournament

import akka.actor.{ Props, ActorRef, ActorSelection, ActorSystem }
import akka.pattern.{ ask, pipe }
import chess.Mode
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.duration._
import scalaz.NonEmptyList

import actorApi._
import lila.common.Debouncer
import lila.db.api._
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.lobby.{ ReloadTournaments, ReloadSimuls }
import lila.hub.actorApi.map.{ Tell, TellIds }
import lila.hub.actorApi.router.Tourney
import lila.hub.Sequencer
import lila.round.actorApi.round.{ ResignColor, GoBerserk, TournamentStanding }
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short

private[tournament] final class TournamentApi(
    system: ActorSystem,
    sequencers: ActorRef,
    autoPairing: AutoPairing,
    router: ActorSelection,
    renderer: ActorSelection,
    timeline: ActorSelection,
    socketHub: ActorRef,
    site: ActorSelection,
    lobby: ActorSelection,
    roundMap: ActorRef,
    trophyApi: lila.user.TrophyApi,
    roundSocketHub: ActorSelection) {

  def makePairings(oldTour: Started, pairings: NonEmptyList[Pairing], postEvents: Events) {
    sequence(oldTour.id) {
      TournamentRepo startedById oldTour.id flatMap {
        case Some(t) =>
          val tour = t addPairings pairings addEvents postEvents
          TournamentRepo.update(tour).void >> (pairings map autoPairing(tour)).sequence map {
            _.list foreach { game =>
              game.tournamentId foreach { tid =>
                sendTo(tid, StartGame(game))
              }
            }
          }
        case None => fufail("Can't make pairings of missing tournament " + oldTour.id)
      }
    }
  }

  def createTournament(setup: TournamentSetup, me: User): Fu[Created] =
    TournamentRepo withdraw me.id flatMap { withdrawIds =>
      val created = Tournament.make(
        createdBy = me,
        clock = TournamentClock(setup.clockTime * 60, setup.clockIncrement),
        minutes = setup.minutes,
        waitMinutes = setup.waitMinutes,
        mode = setup.mode.fold(Mode.default)(Mode.orDefault),
        `private` = setup.`private`.isDefined,
        system = System orDefault setup.system,
        variant = chess.variant.Variant orDefault setup.variant)
      TournamentRepo.insert(created).void >>-
        (withdrawIds foreach socketReload) >>-
        publish() >>- {
          import lila.hub.actorApi.timeline.{ Propagate, TourJoin }
          timeline ! (Propagate(TourJoin(me.id, created.id, created.fullName)) toFollowersOf me.id)
        } inject created
    }

  private[tournament] def createScheduled(schedule: Schedule): Funit =
    (Schedule durationFor schedule) ?? { minutes =>
      val created = Tournament.schedule(schedule, minutes)
      TournamentRepo.insert(created).void >>- publish()
    }

  def startOrDelete(created: Created) {
    if (created.enoughPlayersToStart) start(created) else wipe(created)
  }

  def start(oldTour: Created) {
    sequence(oldTour.id) {
      TournamentRepo createdById oldTour.id flatMap {
        case Some(created) =>
          val started = created.start
          TournamentRepo.update(started).void >>-
            sendTo(started.id, Reload) >>-
            publish()
        case None => fufail("Can't start missing tournament " + oldTour.id)
      }
    }
  }

  def wipe(created: Created): Funit =
    TournamentRepo.remove(created).void >>- publish() >>- socketReload(created.id)

  def finish(oldTour: Started) {
    sequence(oldTour.id) {
      TournamentRepo startedById oldTour.id flatMap {
        case Some(started) =>
          if (started.pairings.isEmpty) TournamentRepo.remove(started).void >>- publish()
          else started.readyToFinish ?? {
            val finished = started.finish
            TournamentRepo.update(finished).void >>-
              sendTo(finished.id, Reload) >>-
              publish() >>-
              finished.players.filter(_.score > 0).map { p =>
                UserRepo.incToints(p.id, p.score)
              }.sequenceFu >>
              awardTrophies(finished)
          }
        case None => fufail("Cannot finish missing tournament " + oldTour)
      }
    }
  }

  private def awardTrophies(tour: Finished): Funit =
    if (tour.schedule.??(_.freq == Schedule.Freq.Marathon)) tour.rankedPlayers.map {
      case rp if rp.rank == 1  => trophyApi.award(rp.player.id, _.MarathonWinner)
      case rp if rp.rank <= 10 => trophyApi.award(rp.player.id, _.MarathonTopTen)
      case rp if rp.rank <= 50 => trophyApi.award(rp.player.id, _.MarathonTopFifty)
      case _                   => funit
    }.sequenceFu.void
    else funit

  def join(oldTour: Enterable, me: User) {
    sequence(oldTour.id) {
      TournamentRepo enterableById oldTour.id flatMap {
        case Some(tour) => tour.join(me).future flatMap { tour2 =>
          TournamentRepo withdraw me.id flatMap { withdrawIds =>
            TournamentRepo.update(tour2).void >>- {
              sendTo(tour.id, Joining(me.id))
              (tour.id :: withdrawIds) foreach socketReload
              publish()
              import lila.hub.actorApi.timeline.{ Propagate, TourJoin }
              timeline ! (Propagate(TourJoin(me.id, tour2.id, tour2.fullName)) toFollowersOf me.id)
            }
          }
        }
        case _ => fufail("Cannot join missing tournament " + oldTour.id)
      }
    }
  }

  def withdraw(oldTour: Tournament, userId: String) {
    sequence(oldTour.id) {
      TournamentRepo byId oldTour.id flatMap {
        case Some(created: Created) => (created withdraw userId).fold(
          err => funit,
          tour2 => TournamentRepo.update(tour2).void >>- socketReload(tour2.id) >>- publish()
        )
        case Some(started: Started) => (started withdraw userId).fold(
          err => funit,
          tour2 => TournamentRepo.update(tour2).void >>-
            (tour2.userCurrentPov(userId) ?? { povRef =>
              roundMap ! Tell(povRef.gameId, ResignColor(povRef.color))
            }) >>-
            socketReload(tour2.id) >>-
            publish()
        )
        case _ => fufail("Cannot withdraw from finished or missing tournament " + oldTour.id)
      }
    }
  }

  def berserk(oldTour: Started, userId: String) {
    sequence(oldTour.id) {
      TournamentRepo startedById oldTour.id flatMap {
        _ ?? { tour =>
          (tour userCurrentPairing userId filter { p =>
            (p berserkOf userId) == 0
          }) ?? { pairing =>
            (pairing povRef userId) ?? { povRef =>
              GameRepo pov povRef flatMap {
                _.filter(_.game.berserkable) ?? { pov =>
                  val tour2 = tour.updatePairing(pov.gameId, _ withBerserk userId)
                  TournamentRepo.update(tour2).void >>- {
                    roundMap ! Tell(povRef.gameId, GoBerserk(povRef.color))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def finishGame(game: Game) {
    game.tournamentId foreach { tourId =>
      sequence(tourId) {
        TournamentRepo startedById tourId flatMap {
          _ ?? { tour =>
            val tour2 = tour.updatePairing(game.id, _ finish game).refreshPlayers
            TournamentRepo.update(tour2).void >>- {
              game.loserUserId.filter(tour2.quickLossStreak) foreach { withdraw(tour2, _) }
            } >>- socketReload(tour2.id) >>- updateTournamentStanding(tour2)
          }
        }
      }
    }
  }

  def ejectCheater(userId: String) {
    TournamentRepo.allEnterable foreach {
      _ foreach { oldTour =>
        sequence(oldTour.id) {
          TournamentRepo enterableById oldTour.id flatMap {
            _ ?? { tour =>
              (tour ejectCheater userId) ?? { tour2 =>
                TournamentRepo.update(tour2).void >>- socketReload(tour2.id)
              }
            }
          }
        }
      }
    }
  }

  private def sequence(tourId: String)(work: => Funit) {
    sequencers ! Tell(tourId, Sequencer work work)
  }

  private def socketReload(tourId: String) {
    sendTo(tourId, Reload)
  }

  private object publish {
    private val siteMessage = SendToFlag("tournament", Json.obj("t" -> "reload"))
    private val debouncer = system.actorOf(Props(new Debouncer(5 seconds, {
      (_: Debouncer.Nothing) =>
        site ! siteMessage
        TournamentRepo.promotable foreach { tours =>
          renderer ? TournamentTable(tours) map {
            case view: play.twirl.api.Html => ReloadTournaments(view.body)
          } pipeToSelection lobby
        }
    })))
    def apply() { debouncer ! Debouncer.Nothing }
  }

  private object updateTournamentStanding {
    private val debouncer = system.actorOf(Props(new Debouncer(5 seconds, {
      (tour: Tournament) =>
        roundSocketHub ! TellIds(tour.playingPairings.map(_.gameId), TournamentStanding(tour.id))
    })))
    def apply(tour: Tournament) {
      debouncer ! tour
    }
  }

  private def sendTo(tourId: String, msg: Any) {
    socketHub ! Tell(tourId, msg)
  }
}
