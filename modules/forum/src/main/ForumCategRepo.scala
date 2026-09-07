package lila.forum

import lila.db.dsl.{ *, given }
import lila.core.perm.Granter

final private class ForumCategRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: ForumCategId) = coll.byId[ForumCateg](id)

  def visibleWithTeams(teams: Iterable[TeamId], forUser: Option[User]): Fu[List[ForumCateg]] =
    val (isMod, isDev) = forUser.fold((false, false)): u =>
      (Granter.of(_.ModerateForum)(u), Granter.of(_.Diagnostics)(u))
    coll
      .find(
        $or(
          List(
            ($doc("team".$exists(false)) ++ (!isMod).so($doc("hidden".$ne(true)))).some,
            teams.nonEmpty.option($doc("team".$in(teams))),
            isDev.option($id(ForumCateg.diagnosticId))
          ).flatten*
        )
      )
      .cursor[ForumCateg](ReadPref.sec)
      .list(100)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts").dmap(~_)
