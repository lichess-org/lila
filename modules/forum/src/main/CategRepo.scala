package lila.forum

import lila.db.dsl._

final class CategRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers.CategBSONHandler

  def bySlug(slug: String) = coll.byId[Categ](slug)

  def withTeams(teams: Iterable[String]): Fu[List[Categ]] =
    coll
      .find(
        $or(
          "team" $exists false,
          $doc("team" $in teams)
        )
      )
      .sort($sort asc "pos")
      .cursor[Categ]()
      .list()

  def nextPosition: Fu[Int] =
    coll.primitiveOne[Int]($empty, $sort desc "pos", "pos") dmap (~_ + 1)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts") dmap (~_)
}
