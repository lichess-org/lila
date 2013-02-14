package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._
import scalaz.{ NonEmptyList, Success, Failure }
import play.api.libs.json._

import chess.{ Mode, Variant }
import controllers.routes
import game.DbGame
import user.User
import lobby.{ Socket ⇒ LobbySocket }

private[tournament] final class TournamentApi(
    repo: TournamentRepo,
    roomRepo: RoomRepo,
    joiner: GameJoiner,
    socket: Socket,
    siteSocket: site.Socket,
    lobbySocket: LobbySocket,
    roundMeddler: round.Meddler,
    incToints: String ⇒ Int ⇒ IO[Unit]) {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): IO[Unit] =
    (tour addPairings pairings) |> { tour2 ⇒
      for {
        _ ← repo saveIO tour2
        games ← (pairings map joiner(tour)).sequence
        _ ← (games map socket.notifyPairing).sequence
      } yield ()
    }

  def createTournament(setup: TournamentSetup, me: User): IO[Created] = for {
    withdrawIds ← repo withdraw me.id
    created = Tournament(
      createdBy = me,
      clock = TournamentClock(setup.clockTime * 60, setup.clockIncrement),
      minutes = setup.minutes,
      minPlayers = setup.minPlayers,
      mode = Mode orDefault ~setup.mode,
      variant = Variant orDefault setup.variant)
    _ ← repo saveIO created
    _ ← (withdrawIds map socket.reload).sequence
    _ ← reloadSiteSocket
    _ ← lobbyReload
    _ ← sendLobbyMessage(created)
  } yield created

  def startIfReady(created: Created): Option[IO[Unit]] = created.startIfReady map doStart

  def earlyStart(created: Created): Option[IO[Unit]] =
    created.readyToEarlyStart option doStart(created.start)

  private def doStart(started: Started): IO[Unit] =
    (repo saveIO started) >> (socket start started.id) >> reloadSiteSocket >> lobbyReload

  def wipeEmpty(created: Created): IO[Unit] = (for {
    _ ← repo removeIO created
    _ ← roomRepo removeIO created.id
    _ ← reloadSiteSocket
    _ ← lobbyReload
    _ ← removeLobbyMessage(created)
  } yield ()) doIf created.isEmpty

  def finish(started: Started): IO[Tournament] = started.readyToFinish.fold({
    val pairingsToAbort = started.playingPairings
    val finished = started.finish
    for {
      _ ← repo saveIO finished
      _ ← socket reloadPage finished.id
      _ ← reloadSiteSocket
      _ ← (pairingsToAbort map (_.gameId) map roundMeddler.forceAbort).sequence
      _ ← finished.players.filter(_.score > 0).map(p ⇒ incToints(p.id)(p.score)).sequence
    } yield finished
  }, io(started))

  def join(tour: Created, me: User): Valid[IO[Unit]] = for {
    tour2 ← tour join me
  } yield for {
    withdrawIds ← repo withdraw me.id
    _ ← repo saveIO tour2
    _ ← socket.notifyJoining(tour.id, me.id)
    _ ← ((tour.id :: withdrawIds) map socket.reload).sequence
    _ ← reloadSiteSocket
    _ ← lobbyReload
  } yield ()

  def withdraw(tour: Tournament, userId: String): IO[Unit] = tour match {
    case created: Created ⇒ (created withdraw userId).fold(
      err ⇒ putStrLn(err.shows) inject tour,
      tour2 ⇒ for {
        _ ← repo saveIO tour2
        _ ← socket reload tour2.id
        _ ← reloadSiteSocket
        _ ← lobbyReload
      } yield ()
    )
    case started: Started ⇒ (started withdraw userId).fold(
      err ⇒ putStrLn(err.shows),
      tour2 ⇒ for {
        _ ← repo saveIO tour2
        _ ← (tour2 userCurrentPov userId).fold(roundMeddler.resign, io())
        _ ← socket reload tour2.id
        _ ← reloadSiteSocket
      } yield ()
    )
    case finished: Finished ⇒ putStrLn("Cannot withdraw from finished tournament " + finished.id) 
  }

  def finishGame(game: DbGame): IO[Option[Tournament]] = for {
    tourOption ← game.tournamentId.fold(repo.startedById, io(None))
    result ← ~tourOption.filter(_ ⇒ game.finished).map(tour ⇒ {
      val tour2 = tour.updatePairing(game.id, _.finish(game.status, game.winnerUserId, game.turns))
      (repo saveIO tour2) >>
        tripleQuickLossWithdraw(tour2, game.loserUserId) inject tour2.some
    })
  } yield result

  private def tripleQuickLossWithdraw(tour: Started, loser: Option[String]): IO[Unit] =
    ~loser.filter(tour.quickLossStreak).map(withdraw(tour, _))

  private def userIdWhoLostOnTimeWithoutMoving(game: DbGame): Option[String] =
    game.playerWhoDidNotMove
      .flatMap(_.userId)
      .filter(_ ⇒ List(chess.Status.Timeout, chess.Status.Outoftime) contains game.status)

  private def lobbyReload = repo.created flatMap { tours ⇒
    lobbySocket reloadTournaments views.html.tournament.createdTable(tours).toString
  }

  private val reloadMessage = JsObject(Seq("t" -> JsString("reload"), "d" -> JsNull))
  private def sendToSiteSocket(message: JsObject) = io {
    siteSocket.sendToFlag("tournament", message)
  }
  private val reloadSiteSocket = sendToSiteSocket(reloadMessage)

  private def sendLobbyMessage(tour: Created) = lobbySocket sysTalk {
    """<a href="%s">%s tournament created</a>""".format(routes.Tournament.show(tour.id), tour.name)
  }

  private def removeLobbyMessage(tour: Created) = lobbySocket unTalk {
    ("%s tournament created" format tour.name).r
  }

}
