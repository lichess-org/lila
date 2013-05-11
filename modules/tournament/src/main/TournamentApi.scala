package lila.tournament

import actorApi._
import tube.roomTube
import tube.tournamentTubes._
import lila.db.api._
import chess.{ Mode, Variant }
import lila.game.Game
import lila.user.User
import lila.hub.actorApi.lobby.{ SysTalk, UnTalk, ReloadTournaments }
import lila.hub.actorApi.router.Tourney
import lila.socket.actorApi.SendToFlag
import makeTimeout.short

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.NonEmptyList
import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import play.api.libs.json._

private[tournament] final class TournamentApi(
    joiner: GameJoiner,
    router: ActorRef,
    renderer: ActorRef,
    socketHub: ActorRef,
    site: ActorRef,
    lobby: ActorRef,
    roundMeddler: lila.round.Meddler,
    incToints: String ⇒ Int ⇒ Funit) {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): Funit =
    (tour addPairings pairings) |> { tour2 ⇒
      $update(tour2) >> (pairings map joiner(tour2)).sequence
    } map {
      _.list foreach { game ⇒
        game.tournamentId foreach { tid ⇒
          socketHub ! Forward(tid, StartGame(game))
        }
      }
    }

  def createTournament(setup: TournamentSetup, me: User): Fu[Created] =
    TournamentRepo withdraw me.id flatMap { withdrawIds ⇒
      val created = Tournament.make(
        createdBy = me,
        clock = TournamentClock(setup.clockTime * 60, setup.clockIncrement),
        minutes = setup.minutes,
        minPlayers = setup.minPlayers,
        mode = Mode orDefault ~setup.mode,
        variant = Variant orDefault setup.variant)
      $insert(created) >>
        (withdrawIds map socket.reload).sequence >>
        reloadSiteSocket >>
        lobbyReload >>
        sendLobbyMessage(created) inject created
    }

  def startIfReady(created: Created): Option[Fu[Unit]] = created.startIfReady map doStart

  def earlyStart(created: Created): Option[Fu[Unit]] =
    created.readyToEarlyStart option doStart(created.start)

  private def doStart(started: Started): Fu[Unit] =
    $update(started) >> (socket start started.id) >> reloadSiteSocket >> lobbyReload

  def wipeEmpty(created: Created): Fu[Unit] = (for {
    _ ← $remove(created)
    _ ← $remove[Room](created.id)
    _ ← reloadSiteSocket
    _ ← lobbyReload
    _ ← removeLobbyMessage(created)
  } yield ()) doIf created.isEmpty

  def finish(started: Started): Fu[Tournament] = started.readyToFinish.fold({
    val pairingsToAbort = started.playingPairings
    val finished = started.finish
    for {
      _ ← $update(finished)
      _ ← socket reloadPage finished.id
      _ ← reloadSiteSocket
      _ ← (pairingsToAbort map (_.gameId) map roundMeddler.forceAbort).sequence
      _ ← finished.players.filter(_.score > 0).map(p ⇒ incToints(p.id)(p.score)).sequence
    } yield finished
  }, fuccess(started))

  def join(tour: Created, me: User): Funit =
    (tour join me).future flatMap { tour2 ⇒
      TournamentRepo withdraw me.id flatMap { withdrawIds ⇒
        $update(tour2) >>-
          (socketHub ! Forward(tour.id, Joining(me.id))) >>-
          ((tour.id :: withdrawIds) foreach { tourId ⇒
            socketHub ! Forward(tourId, Reload)
          }) >>-
          reloadSiteSocket >>-
          lobbyReload
      }
    }

  def withdraw(tour: Tournament, userId: String): Funit = tour match {
    case created: Created ⇒ (created withdraw userId).fold(
      err ⇒ fufail(err.shows),
      tour2 ⇒ $update(tour2) >> (socket reload tour2.id) >> reloadSiteSocket >> lobbyReload
    )
    case started: Started ⇒ (started withdraw userId).fold(
      err ⇒ fufail(err.shows),
      tour2 ⇒ $update(tour2) >>
        ~(tour2 userCurrentPov userId map roundMeddler.resign) >>
        (socket reload tour2.id) >>
        reloadSiteSocket
    )
    case finished: Finished ⇒ fufail("Cannot withdraw from finished tournament " + finished.id)
  }

  def finishGame(game: Game): Fu[Option[Tournament]] = for {
    tourOption ← ~(game.tournamentId map TournamentRepo.startedById)
    result ← ~(tourOption.filter(_ ⇒ game.finished).map(tour ⇒ {
      val tour2 = tour.updatePairing(game.id, _.finish(game.status, game.winnerUserId, game.turns))
      $update(tour2) >> tripleQuickLossWithdraw(tour2, game.loserUserId) inject tour2.some
    }))
  } yield result

  private def tripleQuickLossWithdraw(tour: Started, loser: Option[String]): Funit =
    loser.filter(tour.quickLossStreak).zmap(withdraw(tour, _))

  private def userIdWhoLostOnTimeWithoutMoving(game: Game): Option[String] =
    game.playerWhoDidNotMove
      .flatMap(_.userId)
      .filter(_ ⇒ List(chess.Status.Timeout, chess.Status.Outoftime) contains game.status)

  private def lobbyReload {
    TournamentRepo.created foreach { tours ⇒
      renderer ? TournamentTable(tours) map {
        case view: play.api.templates.Html ⇒ ReloadTournaments(view)
      } pipeTo lobby
    }
  }

  private val reloadMessage = Json.obj("t" -> "reload", "d" -> JsNull)
  private def reloadSiteSocket {
    site ! SendToFlag("tournament", reloadMessage)
  }

  private def sendLobbyMessage(tour: Created) {
    router ? Tourney(tour.id) map {
      case url: String ⇒ SysTalk(
        """<a href="%s">%s tournament created</a>""".format(url, tour.name)
      )
    } pipeTo lobby
  }

  private def removeLobbyMessage(tour: Created) {
    lobby ! UnTalk("%s tournament created".format(tour.name).r)
  }

}
