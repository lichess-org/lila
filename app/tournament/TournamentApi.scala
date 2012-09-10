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

  def makePairings(tour: Started, pairings: NonEmptyList[Pairing]): IO[Unit] = {
    io()
  }

  def createTournament(setup: TournamentSetup, me: User): IO[Created] =
    Tournament(
      createdBy = me.id,
      minutes = setup.minutes,
      minUsers = setup.minUsers
    ) |> { created ⇒
        repo saveIO created map (_ ⇒ created)
      }

  def start(created: Created): IO[Unit] = repo saveIO created.start

  def join(tour: Created, me: User): IO[Unit] = (tour join me).fold(
    err ⇒ putStrLn(err.shows),
    tour2 ⇒ for {
      _ ← repo saveIO tour2
      _ ← io(socket reloadUserList tour.id)
    } yield ()
  )

  def withdraw(tour: Created, me: User): IO[Unit] = (tour withdraw me).fold(
    err ⇒ putStrLn(err.shows),
    tour2 ⇒ for {
      _ ← repo saveIO tour2
      _ ← io(socket reloadUserList tour.id)
    } yield ()
  )
}
