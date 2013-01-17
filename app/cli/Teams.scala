package lila
package cli

import user.{ User, UserRepo }
import team.{ Team, TeamRepo, TeamApi, SearchIndexer }
import scalaz.effects._

private[cli] case class Teams(
    teamRepo: TeamRepo,
    userRepo: UserRepo,
    indexer: SearchIndexer,
    api: TeamApi) {

  def searchReset: IO[String] = indexer.rebuildAll inject "Search index reset"

  def join(teamId: String, userIds: List[String]): IO[String] =
    perform2(teamId, userIds)(api.doJoin _)

  def quit(teamId: String, userIds: List[String]): IO[String] =
    perform2(teamId, userIds)(api.doQuit _)

  def enable(teamId: String): IO[String] = perform(teamId)(teamRepo.enable _)

  def disable(teamId: String): IO[String] = perform(teamId)(teamRepo.disable _)

  def delete(teamId: String): IO[String] = perform(teamId)(api.delete _)

  private def perform(teamId: String)(op: Team ⇒ IO[Unit]) = for {
    teamOption ← teamRepo byId teamId.pp
    res ← teamOption.fold(
      u ⇒ op(u) inject "Success",
      io("Team not found")
    )
  } yield res

  private def perform2(teamId: String, userIds: List[String])(op: (Team, String) ⇒ IO[Unit]) = for {
    teamOption ← teamRepo byId teamId
    res ← teamOption.fold(
      team ⇒ for {
        users ← userRepo byIds userIds
        _ ← users.map(user ⇒ putStrLn(user.username) >> op(team, user.id)).sequence
      } yield "Success",
      io("Team not found")
    )
  } yield res
}
