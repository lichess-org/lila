package lila.forum

import lila.db.dsl.{ *, given }
import reactivemongo.api.ReadPreference

final private class ForumCategRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: ForumCategId) = coll.byId[ForumCateg](id)

  def visibleWithTeams(teams: Iterable[TeamId]): Fu[List[ForumCateg]] =
    coll
      .find(
        $or(
          $doc("hidden" $ne true, "team" $exists false),
          $doc("team" $in teams)
        )
      )
      .cursor[ForumCateg](ReadPreference.secondaryPreferred)
      .list(100)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts").dmap(~_)
