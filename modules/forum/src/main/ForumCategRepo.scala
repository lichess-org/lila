package lila.forum

import lila.db.dsl.{ *, given }

final private class ForumCategRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: ForumCategId) = coll.byId[ForumCateg](id)

  def visibleWithTeams(teams: Iterable[TeamId], isMod: Boolean = false): Fu[List[ForumCateg]] =
    coll
      .find(
        $or(
          $doc("team".$exists(false)) ++ (!isMod).so($doc("hidden".$ne(true))),
          $doc("team".$in(teams))
        )
      )
      .cursor[ForumCateg](ReadPref.sec)
      .list(100)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts").dmap(~_)
