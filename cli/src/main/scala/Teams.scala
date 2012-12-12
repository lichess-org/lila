package lila
package cli

import user.{ User, UserRepo }
import team.{ Team, TeamRepo, TeamApi }
import scalaz.effects._

case class Teams(
    teamRepo: TeamRepo,
    userRepo: UserRepo,
    api: TeamApi) {

  def join(teamId: String, userIds: List[String]): IO[Unit] =
    perform(teamId, userIds) { (team: Team, user: User) ⇒
      api.doJoin(team, user)
    }

  private def perform(teamId: String, userIds: List[String])(op: (Team, User) ⇒ IO[Unit]) = for {
    teamOption ← teamRepo byId teamId
    _ ← teamOption.fold(
      team ⇒ for {
        users ← userRepo byIds userIds
        _ ← users.map(user ⇒ putStrLn(user.username) >> op(team, user)).sequence
      } yield (),
      putStrLn("Team not found")
    )
  } yield ()
}
