package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._
import scalaz.{ NonEmptyList, Success, Failure }
import play.api.libs.json._

import game.{ DbGame, DbPlayer, GameRepo }
import user.User

final class TournamentApi(
    repo: TournamentRepo,
    gameRepo: GameRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    getUser: String ⇒ IO[Option[User]],
    socket: Socket,
    siteSocket: site.Socket) {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): IO[Unit] =
    (tour addPairings pairings) |> { tour2 ⇒
      for {
        _ ← repo saveIO tour2
        games ← (pairings map makeGame(tour)).sequence
        _ ← (games map socket.notifyPairing).sequence
      } yield ()
    }

  def createTournament(setup: TournamentSetup, me: User): IO[Created] = for {
    withdrawIds ← repo withdraw me
    created = Tournament(
      createdBy = me,
      clock = TournamentClock(setup.clockTime, setup.clockIncrement),
      minutes = setup.minutes,
      minPlayers = setup.minPlayers)
    _ ← repo saveIO created
    _ ← (withdrawIds map socket.reload).sequence
    _ ← reloadSiteSocket
  } yield created

  def start(created: Created): IO[Unit] = (for {
    _ ← repo saveIO created.start
    _ ← socket reload created.id
    _ ← reloadSiteSocket
  } yield ()) doIf created.readyToStart

  def finish(started: Started): IO[Unit] = (for {
    _ ← repo saveIO started.finish
    _ ← socket reload started.id
    _ ← reloadSiteSocket
  } yield ()) doIf started.readyToFinish

  def join(tour: Created, me: User): Valid[IO[Unit]] = for {
    tour2 ← tour join me
  } yield for {
    withdrawIds ← repo withdraw me
    _ ← repo saveIO tour2
    _ ← ((tour.id :: withdrawIds) map socket.reload).sequence
    _ ← reloadSiteSocket
  } yield ()

  def withdraw(tour: Tournament, me: User): IO[Unit] = (tour match {
    case created: Created   ⇒ created withdraw me
    case started: Started   ⇒ started withdraw me
    case finished: Finished ⇒ !!("Cannot withdraw from finished tournament " + finished.id)
  }).fold(
    err ⇒ putStrLn(err.shows),
    tour2 ⇒ for {
      _ ← repo saveIO tour2
      _ ← socket reload tour2.id
      _ ← reloadSiteSocket
    } yield ()
  )

  def finishGame(gameId: String): IO[Option[Tournament]] = for {
    gameOption ← gameRepo game gameId
    tourOption ← gameOption flatMap (_.tournamentId) fold (repo.startedById, io(None))
    result ← {
      (gameOption |@| tourOption) apply { (game: DbGame, tour: Started) ⇒
        repo saveIO tour.updatePairing(
          game.id,
          _.finish(game.status, game.winnerUserId)
        ) map (_ ⇒ tour.some)
      }
    } | io(none)
  } yield result

  private val reloadMessage = JsObject(Seq("t" -> JsString("reload"), "d" -> JsNull))
  private def sendToSiteSocket(message: JsObject) = io {
    siteSocket.sendToFlag("tournament", message)
  }
  private val reloadSiteSocket = sendToSiteSocket(reloadMessage)

  private def makeGame(tour: Started)(pairing: Pairing): IO[DbGame] = for {
    user1 ← getUser(pairing.user1) map (_ err "No such user " + pairing)
    user2 ← getUser(pairing.user2) map (_ err "No such user " + pairing)
    variant = chess.Variant.Standard
    game = DbGame(
      game = chess.Game(
        board = chess.Board init variant,
        clock = tour.clock.chessClock.some
      ),
      ai = None,
      whitePlayer = DbPlayer.white withUser user1,
      blackPlayer = DbPlayer.black withUser user2,
      creatorColor = chess.Color.White,
      mode = chess.Mode.Rated,
      variant = variant
    ).withTournamentId(tour.id)
      .withId(pairing.gameId)
      .start
      .startClock(2)
    _ ← gameRepo insert game
    _ ← gameRepo denormalizeStarted game
    _ ← timelinePush(game)
  } yield game
}
