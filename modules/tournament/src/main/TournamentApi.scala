package lila.tournament

import akka.actor.{ ActorRef, ActorSelection }
import akka.pattern.{ ask, pipe }
import chess.{ Mode, Variant }
import org.joda.time.DateTime
import play.api.libs.json._
import scalaz.NonEmptyList

import actorApi._
import lila.db.api._
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.lobby.ReloadTournaments
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.router.Tourney
import lila.hub.Sequencer
import lila.round.actorApi.round.ResignColor
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short

private[tournament] final class TournamentApi(
    sequencers: ActorRef,
    autoPairing: AutoPairing,
    router: ActorSelection,
    renderer: ActorSelection,
    timeline: ActorSelection,
    socketHub: ActorRef,
    site: ActorSelection,
    lobby: ActorSelection,
    onStart: String => Unit,
    roundMap: ActorRef) {

  def makePairings(oldTour: Started, pairings: NonEmptyList[Pairing], postEvents: Events) {
    sequence(oldTour.id) {
      TournamentRepo startedById oldTour.id flatMap {
        case Some(tour) =>
          val tour2 = tour addPairings pairings
          val tour3 = if (postEvents.isEmpty) tour2 else { tour2 addEvents postEvents }

          TournamentRepo.update(tour3).void >> (pairings map autoPairing(tour3)).sequence map {
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
        minPlayers = setup.minPlayers,
        mode = setup.mode.fold(Mode.default)(Mode.orDefault),
        password = setup.password,
        system = System orDefault setup.system,
        variant = Variant orDefault setup.variant)
      TournamentRepo.insert(created).void >>-
        (withdrawIds foreach socketReload) >>-
        reloadSiteSocket >>-
        lobbyReload inject created
    }

  def createScheduled(schedule: Schedule): Funit =
    (Schedule durationFor schedule) ?? { minutes =>
      val created = Tournament.schedule(schedule, minutes)
      TournamentRepo.insert(created).void >>- reloadSiteSocket >>- lobbyReload
    }

  def startIfReady(created: Created) {
    if (created.enoughPlayersToEarlyStart) doStart(created)
  }

  private[tournament] def startScheduled(created: Created) {
    doStart(created)
  }

  private def doStart(oldTour: Created) {
    sequence(oldTour.id) {
      TournamentRepo createdById oldTour.id flatMap {
        case Some(created) =>
          val started = created.start
          TournamentRepo.update(started).void >>-
            sendTo(started.id, Start) >>-
            reloadSiteSocket >>-
            lobbyReload >>- onStart(created.id)
        case None => fufail("Can't start missing tournament " + oldTour.id)
      }
    }
  }

  def wipeEmpty(created: Created): Funit = created.isEmpty ?? doWipe(created)

  private def doWipe(created: Created): Funit =
    TournamentRepo.remove(created).void >>- reloadSiteSocket >>- lobbyReload

  def finish(oldTour: Started) {
    sequence(oldTour.id) {
      TournamentRepo startedById oldTour.id flatMap {
        case Some(started) =>
          if (started.pairings.isEmpty) TournamentRepo.remove(started).void >>- reloadSiteSocket >>- lobbyReload
          else started.readyToFinish ?? {
            val finished = started.finish
            TournamentRepo.update(finished).void >>-
              sendTo(finished.id, ReloadPage) >>-
              reloadSiteSocket >>-
              finished.players.filter(_.score > 0).map { p =>
                UserRepo.incToints(p.id, p.score)
              }.sequenceFu
          }
        case None => fufail("Cannot finish missing tournament " + oldTour)
      }
    }
  }

  def join(oldTour: Enterable, me: User, password: Option[String]) {
    sequence(oldTour.id) {
      TournamentRepo enterableById oldTour.id flatMap {
        case Some(tour) => (tour.join(me, password)).future flatMap { tour2 =>
          TournamentRepo withdraw me.id flatMap { withdrawIds =>
            TournamentRepo.update(tour2).void >>- {
              sendTo(tour.id, Joining(me.id))
              (tour.id :: withdrawIds) foreach socketReload
              reloadSiteSocket
              lobbyReload
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
          err => fulogwarn(err.shows),
          tour2 => TournamentRepo.update(tour2).void >>- socketReload(tour2.id) >>- reloadSiteSocket >>- lobbyReload
        )
        case Some(started: Started) => (started withdraw userId).fold(
          err => fufail(err.shows),
          tour2 => TournamentRepo.update(tour2).void >>-
            (tour2.userCurrentPov(userId) ?? { povRef =>
              roundMap ! Tell(povRef.gameId, ResignColor(povRef.color))
            }) >>-
            socketReload(tour2.id) >>-
            reloadSiteSocket
        )
        case _ => fufail("Cannot withdraw from finished or missing tournament " + oldTour.id)
      }
    }
  }

  def finishGame(game: Game) {
    game.tournamentId foreach { tourId =>
      sequence(tourId) {
        TournamentRepo startedById tourId flatMap {
          _ ?? { tour =>
            val tour2 = tour.updatePairing(game.id, _.finish(game.status, game.winnerUserId, game.turns))
            TournamentRepo.update(tour2).void >>- {
              game.loserUserId.filter(tour2.quickLossStreak) foreach { withdraw(tour2, _) }
            } >>- socketReload(tour2.id)
          }
        }
      }
    }
  }

  private def lobbyReload {
    TournamentRepo.promotable foreach { tours =>
      renderer ? TournamentTable(tours) map {
        case view: play.twirl.api.Html => ReloadTournaments(view.body)
      } pipeToSelection lobby
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

  private val reloadMessage = Json.obj("t" -> "reload")
  private def reloadSiteSocket {
    site ! SendToFlag("tournament", reloadMessage)
  }

  private def sendTo(tourId: String, msg: Any) {
    socketHub ! Tell(tourId, msg)
  }
}
