package lila.forum

import lila.db.dsl.{ *, given }

final private class ForumCategRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: ForumCategId) = coll.byId[ForumCateg](id)

  def visibleWithTeams(teams: Iterable[TeamId], isMod: Boolean = false): Fu[List[ForumCateg]] =
    val notTeam = $doc("team" $exists false)
    coll
      .find(
        $or(
          if isMod then notTeam else notTeam ++ $doc("hidden" $ne true),
          $doc("team" $in teams)
        )
      )
      .cursor[ForumCateg](ReadPref.sec)
      .list(100)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts").dmap(~_)
