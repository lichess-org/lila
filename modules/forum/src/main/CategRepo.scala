package lila.forum

import lila.db.dsl.{ *, given }
import reactivemongo.api.ReadPreference

final class CategRepo(val coll: Coll)(using ec: scala.concurrent.ExecutionContext):

  import BSONHandlers.given

  def bySlug(slug: String) = coll.byId[Categ](slug)

  def visibleWithTeams(teams: Iterable[TeamId]): Fu[List[Categ]] =
    coll
      .find(
        $or(
          $doc("hidden" $ne true, "team" $exists false),
          $doc("team" $in teams)
        )
      )
      .cursor[Categ](ReadPreference.secondaryPreferred)
      .list(100)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts").dmap(~_)
