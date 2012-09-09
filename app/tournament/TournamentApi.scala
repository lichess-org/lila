package lila
package tournament

import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import user.User

final class TournamentApi(
    repo: TournamentRepo) {

  def makeTournament(data: TournamentSetup, me: User) = {
    val tournament = Tournament(
      createdBy = me.id,
      startsAt = DateTime.now,
      minutes = data.minutes)
    for {
      _ ← repo saveIO tournament
    } yield tournament
  }
}
