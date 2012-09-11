package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._
import scalaz.{ NonEmptyList, Success, Failure }

import game.{ DbGame, DbPlayer, GameRepo }
import user.User

final class TournamentApi(
    repo: TournamentRepo,
    gameRepo: GameRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    getUser: String ⇒ IO[Option[User]],
    socket: Socket) {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): IO[Unit] =
    (tour addPairings pairings) |> { tour2 ⇒
      for {
        _ ← repo saveIO tour2
        games ← (pairings map makeGame(tour.id)).sequence
        _ ← (games map socket.notifyPairing).sequence
      } yield ()
    }

  def createTournament(setup: TournamentSetup, me: User): IO[Valid[Created]] = for {
    hasTournament ← repo userHasRunningTournament me.id
    tournament ← hasTournament.fold(
      io(alreadyInATournament(me)),
      Tournament(
        createdBy = me.id,
        minutes = setup.minutes,
        minUsers = setup.minUsers
      ) |> { created ⇒ repo saveIO created map (_ ⇒ created.success) }
    )
  } yield tournament

  def start(created: Created): IO[Unit] =
    repo saveIO created.start doIf created.readyToStart

  def join(tour: Created, me: User): IO[Valid[Unit]] = for {
    hasTournament ← repo userHasRunningTournament me.id
    tour2Valid = for {
      tour2 ← tour join me
      _ ← hasTournament.fold(alreadyInATournament(me), true.success)
    } yield tour2
    result ← tour2Valid.fold(
      err ⇒ io(failure(err)),
      tour2 ⇒ for {
        _ ← repo saveIO tour2
        _ ← io(socket reloadUserList tour.id)
      } yield ().success
    ): IO[Valid[Unit]]
  } yield result

  def withdraw(tour: Created, me: User): IO[Unit] = (tour withdraw me).fold(
    err ⇒ putStrLn(err.shows),
    tour2 ⇒ for {
      _ ← repo saveIO tour2
      _ ← socket reloadUserList tour.id
    } yield ()
  )

  def finishGame(gameId: String): IO[Unit] = for {
    gameOption ← gameRepo game gameId
    tourOption ← gameOption flatMap (_.tournamentId) fold (repo.startedById, io(None))
    _ ← {
      (gameOption |@| tourOption) apply { (game: DbGame, tour: Started) ⇒
        repo saveIO tour.updatePairing(game.id, _.finish(game.status, game.winnerUserId))
      }
    } | io()
  } yield ()

  private def makeGame(tournamentId: String)(pairing: Pairing): IO[DbGame] = for {
    user1 ← getUser(pairing.user1) map (_ err "No such user " + pairing)
    user2 ← getUser(pairing.user2) map (_ err "No such user " + pairing)
    variant = chess.Variant.Standard
    game = DbGame(
      game = chess.Game(
        board = chess.Board init variant,
        clock = chess.Clock(120, 0).some
      ),
      ai = None,
      whitePlayer = DbPlayer.white withUser user1,
      blackPlayer = DbPlayer.black withUser user2,
      creatorColor = chess.Color.White,
      mode = chess.Mode.Rated,
      variant = variant
    ).withTournamentId(tournamentId)
      .withId(pairing.gameId)
      .start
      .startClock(2)
    _ ← gameRepo insert game
    _ ← timelinePush(game)
  } yield game

  private def alreadyInATournament[A](user: User) =
    !![A]("%s already has a tournament in progress" format user.username)
}
