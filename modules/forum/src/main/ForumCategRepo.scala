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
        or(
          List(
            ("team".exists(false) ++ (!isMod).so(bdoc("hidden".neq(true)))).some,
            teams.nonEmpty.option(bdoc("team".in(teams))),
            isDev.option(bid(ForumCateg.diagnosticId))
          ).flatten*
        )
      )
      .cursor[ForumCateg](ReadPref.sec)
      .list(100)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int](bid(id), "nbPosts").dmap(~_)
