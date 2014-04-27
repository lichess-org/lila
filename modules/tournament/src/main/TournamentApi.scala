package lila.tournament

import akka.actor.{ ActorRef, ActorSelection }
import akka.pattern.{ ask, pipe }
import chess.{ Mode, Variant }
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.libs.json._
import scalaz.NonEmptyList

import actorApi._
import lila.db.api._
import lila.game.{ Game, GameRepo }
import lila.hub.actorApi.lobby.ReloadTournaments
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.router.Tourney
import lila.round.actorApi.round.{ AbortForce, ResignColor }
import lila.socket.actorApi.SendToFlag
import lila.user.{ User, UserRepo }
import makeTimeout.short
import tube.tournamentTubes._

private[tournament] final class TournamentApi(
    joiner: GameJoiner,
    router: ActorSelection,
    renderer: ActorSelection,
    socketHub: ActorRef,
    site: ActorSelection,
    lobby: ActorSelection,
    roundMap: ActorRef) {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): Funit =
    (tour addPairings pairings) |> { tour2 =>
      $update(tour2) >> (pairings map joiner(tour2)).sequence
    } map {
      _.list foreach { game =>
        game.tournamentId foreach { tid =>
          sendTo(tid, StartGame(game))
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
        mode = Mode orDefault ~setup.mode,
        password = setup.password,
        variant = Variant orDefault setup.variant)
      $insert(created) >>-
        (withdrawIds foreach socketReload) >>-
        reloadSiteSocket >>-
        lobbyReload inject created
    }

  def createScheduled(schedule: Schedule): Funit =
    (Schedule durationFor schedule) match {
      case None => funit
      case Some(minutes) =>
        val created = Tournament.schedule(schedule, minutes)
        $insert(created) >>- reloadSiteSocket >>- lobbyReload
    }

  def startIfReady(created: Created): Option[Funit] = created.startIfReady map doStart

  def earlyStart(created: Created): Option[Funit] =
    created.enoughPlayersToEarlyStart option doStart(created.start)

  private[tournament] def startScheduled(created: Created) =
    if (created.nbPlayers >= Tournament.minPlayers) doStart(created.start) else doWipe(created)

  private def doStart(started: Started): Funit =
    $update(started) >>-
      sendTo(started.id, Start) >>-
      reloadSiteSocket >>-
      lobbyReload

  def wipeEmpty(created: Created): Funit = created.isEmpty ?? doWipe(created)

  private def doWipe(created: Created): Funit =
    $remove(created) >>- reloadSiteSocket >>- lobbyReload

  def finish(started: Started): Fu[Tournament] =
    if (started.pairings.isEmpty) $remove(started) >>- reloadSiteSocket >>- lobbyReload inject started
    else started.readyToFinish.fold({
      val pairingsToAbort = started.playingPairings
      val finished = started.finish
      $update(finished) >>-
        sendTo(started.id, ReloadPage) >>-
        reloadSiteSocket >>-
        (pairingsToAbort foreach { pairing =>
          roundMap ! Tell(pairing.gameId, AbortForce)
        }) >>
        finished.players.filter(_.score > 0).map(p => UserRepo.incToints(p.id, p.score)).sequenceFu inject finished
    }, fuccess(started))

  def join(tour: Enterable, me: User, password: Option[String]): Funit =
    (tour.join(me, password)).future flatMap { tour2 =>
      TournamentRepo withdraw me.id flatMap { withdrawIds =>
        $update(tour2) >>-
          sendTo(tour.id, Joining(me.id)) >>-
          ((tour.id :: withdrawIds) foreach socketReload) >>-
          reloadSiteSocket >>-
          lobbyReload
      }
    }

  def withdraw(tour: Tournament, userId: String): Funit = tour match {
    case created: Created => (created withdraw userId).fold(
      err => fulogwarn(err.shows),
      tour2 => $update(tour2) >>- socketReload(tour2.id) >>- reloadSiteSocket >>- lobbyReload
    )
    case started: Started => (started withdraw userId).fold(
      err => fufail(err.shows),
      tour2 => $update(tour2) >>-
        (tour2.userCurrentPov(userId) ?? { povRef =>
          roundMap ! Tell(povRef.gameId, ResignColor(povRef.color))
        }) >>-
        socketReload(tour2.id) >>-
        reloadSiteSocket
    )
    case finished: Finished => fufail("Cannot withdraw from finished tournament " + finished.id)
  }

  def finishGame(game: Game): Fu[Option[Tournament]] =
    (game.tournamentId ?? TournamentRepo.startedById) flatMap {
      _ ?? { tour =>
        val tour2 = tour.updatePairing(game.id, _.finish(game.status, game.winnerUserId, game.turns))
        $update(tour2) >>
          tripleQuickLossWithdraw(tour2, game.loserUserId) inject
          tour2.some
      }
    }

  def recountAll = UserRepo.removeAllToints >> funit
    $enumerate.over($query[Finished](TournamentRepo.finishedQuery)) { (tour: Finished) =>
      val tour2 = tour.refreshPlayers
      val players = tour.players.filter(_.score > 0)
      println(s"tour ${tour2.id}: ${players.size}")
      $update(tour2) zip
        players.map(p => UserRepo.incToints(p.id, p.score)).sequenceFu void
    }

  private def tripleQuickLossWithdraw(tour: Started, loser: Option[String]): Funit =
    loser.filter(tour.quickLossStreak).??(withdraw(tour, _))

  private def lobbyReload {
    TournamentRepo.allCreatedSorted foreach { tours =>
      renderer ? TournamentTable(tours) map {
        case view: play.api.templates.Html => ReloadTournaments(view.body)
      } pipeToSelection lobby
    }
  }

  def ejectCheater(userId: String) = TournamentRepo.allEnterable map {
    _.map { tour =>
      (tour ejectCheater userId) ?? { $update(_) }
    }.sequence
  }

  def socketReload(tourId: String) {
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
