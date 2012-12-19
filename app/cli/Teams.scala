package lila
package cli

import user.{ User, UserRepo }
import team.{ Team, TeamRepo, TeamApi }
import scalaz.effects._

private[cli] case class Teams(
    teamRepo: TeamRepo,
    userRepo: UserRepo,
    api: TeamApi) {

  def join(teamId: String, userIds: List[String]): IO[Unit] =
    perform2(teamId, userIds)(api.doJoin _)

  def quit(teamId: String, userIds: List[String]): IO[Unit] =
    perform2(teamId, userIds)(api.doQuit _)

  def enable(teamId: String): IO[Unit] = perform(teamId)(teamRepo.enable _)

  def disable(teamId: String): IO[Unit] = perform(teamId)(teamRepo.disable _)

  def delete(teamId: String): IO[Unit] = perform(teamId)(api.delete _)

  private def perform(teamId: String)(op: Team ⇒ IO[Unit]) = for {
    teamOption ← teamRepo byId teamId.pp
    _ ← teamOption.fold(
      u ⇒ op(u) flatMap { _ ⇒ putStrLn("Success") },
      putStrLn("Team not found")
    )
  } yield ()

  private def perform2(teamId: String, userIds: List[String])(op: (Team, String) ⇒ IO[Unit]) = for {
    teamOption ← teamRepo byId teamId
    _ ← teamOption.fold(
      team ⇒ for {
        users ← userRepo byIds userIds
        _ ← users.map(user ⇒ putStrLn(user.username) >> op(team, user.id)).sequence
      } yield (),
      putStrLn("Team not found")
    )
  } yield ()
}
