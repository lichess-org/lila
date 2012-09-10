package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._
import scalaz.NonEmptyList

import user.User

final class TournamentApi(
    repo: TournamentRepo,
    socket: Socket) {

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): IO[Unit] =
    (tour addPairings pairings) |> { tour2 ⇒
      for {
        _ ← repo saveIO tour2
        _ ← (pairings map { pairing ⇒
          io() // create the game
        }).sequence
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
      _ ← io(socket reloadUserList tour.id)
    } yield ()
  )

  private def alreadyInATournament[A](user: User) = 
    !![A]("%s already has a tournament in progress" format user.username)
}
