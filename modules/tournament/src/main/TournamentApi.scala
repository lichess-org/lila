package lila.tournament

import actorApi._
import tube.roomTube
import tube.tournamentTubes._
import lila.db.api._
import chess.{ Mode, Variant }
import lila.game.{ Game, GameRepo }
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
    incToints: String ⇒ Int ⇒ Funit) extends scalaz.OptionTs {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): Funit =
    (tour addPairings pairings) |> { tour2 ⇒
      $update(tour2) >> (pairings map joiner(tour2)).sequence
    } map {
      _.list foreach { game ⇒
        game.tournamentId foreach { tid ⇒
          sendTo(tid, StartGame(game))
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
      $insert(created) >>-
        (withdrawIds foreach socketReload) >>-
        reloadSiteSocket >>-
        lobbyReload >>-
        sendLobbyMessage(created) inject created
    }

  def startIfReady(created: Created): Option[Funit] = created.startIfReady map doStart

  def earlyStart(created: Created): Option[Funit] =
    created.readyToEarlyStart option doStart(created.start)

  private def doStart(started: Started): Funit =
    $update(started) >>-
      sendTo(started.id, Start) >>-
      reloadSiteSocket >>-
      lobbyReload

  def wipeEmpty(created: Created): Funit = created.isEmpty ?? {
    $remove(created) >>
      $remove.byId[Room](created.id) >>-
      reloadSiteSocket >>-
      lobbyReload >>-
      (lobby ! UnTalk("%s tournament created".format(created.name).r))
  }

  def finish(started: Started): Fu[Tournament] = started.readyToFinish.fold({
    val pairingsToAbort = started.playingPairings
    val finished = started.finish
    $update(finished) >>-
      sendTo(started.id, ReloadPage) >>-
      reloadSiteSocket >>-
      (pairingsToAbort map (_.gameId) foreach roundMeddler.forceAbort) >>
      finished.players.filter(_.score > 0).map(p ⇒ incToints(p.id)(p.score)).sequence inject finished
  }, fuccess(started))

  def join(tour: Created, me: User): Funit =
    (tour join me).future flatMap { tour2 ⇒
      TournamentRepo withdraw me.id flatMap { withdrawIds ⇒
        $update(tour2) >>-
          sendTo(tour.id, Joining(me.id)) >>-
          ((tour.id :: withdrawIds) foreach socketReload) >>-
          reloadSiteSocket >>-
          lobbyReload
      }
    }

  def withdraw(tour: Tournament, userId: String): Funit = tour match {
    case created: Created ⇒ (created withdraw userId).fold(
      err ⇒ fufail(err.shows),
      tour2 ⇒ $update(tour2) >>- socketReload(tour2.id) >>- reloadSiteSocket >>- lobbyReload
    )
    case started: Started ⇒ (started withdraw userId).fold(
      err ⇒ fufail(err.shows),
      tour2 ⇒ $update(tour2) >>-
        (tour2 userCurrentPov userId foreach roundMeddler.resign) >>-
        socketReload(tour2.id) >>-
        reloadSiteSocket
    )
    case finished: Finished ⇒ fufail("Cannot withdraw from finished tournament " + finished.id)
  }

  def finishGame(gameId: String): Fu[Option[Tournament]] = for {
    game ← optionT(GameRepo finished gameId)
    tour ← optionT(game.tournamentId zmap TournamentRepo.startedById)
    result ← optionT {
      val tour2 = tour.updatePairing(game.id, _.finish(game.status, game.winnerUserId, game.turns))
      $update(tour2) >>
        tripleQuickLossWithdraw(tour2, game.loserUserId) inject
        tour2.some
    }
  } yield result.value

  private def tripleQuickLossWithdraw(tour: Started, loser: Option[String]): Funit =
    loser.filter(tour.quickLossStreak).zmap(withdraw(tour, _))

  private def userIdWhoLostOnTimeWithoutMoving(game: Game): Option[String] =
    game.playerWhoDidNotMove
      .flatMap(_.userId)
      .filter(_ ⇒ List(chess.Status.Timeout, chess.Status.Outoftime) contains game.status)

  private def lobbyReload {
    TournamentRepo.created foreach { tours ⇒
      renderer ? TournamentTable(tours) map {
        case view: play.api.templates.Html ⇒ ReloadTournaments(view.body)
      } pipeTo lobby
    }
  }

  def socketReload(tourId: String) {
    sendTo(tourId, Reload)
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

  private def sendTo(tourId: String, msg: Any) {
    socketHub ! Forward(tourId, msg)
  }
}
