package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import scalaz.effects._

import user.User

final class TournamentApi(
    repo: TournamentRepo) {

  def createTournament(setup: TournamentSetup, me: User): IO[Created] = {
    val tournament = Tournament(
      createdBy = me.id,
      minutes = setup.minutes,
      minUsers = setup.minUsers)
    for {
      _ ← repo saveIO tournament
    } yield tournament
  }

  def join(tour: Created, me: User): IO[Unit] = (tour join me).fold(
    err => putStrLn(err.shows),
    tour2 ⇒ repo saveIO tour2
  )
}
