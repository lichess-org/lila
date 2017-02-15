package lila.forum

import lila.db.dsl._

object CategRepo {

  import BSONHandlers.CategBSONHandler

  // dirty
  private val coll = Env.current.categColl

  def bySlug(slug: String) = coll.byId[Categ](slug)

  def withTeams(teams: Iterable[String]): Fu[List[Categ]] =
    coll.find($or(
      "team" $exists false,
      $doc("team" $in teams)
    )).sort($sort asc "pos").cursor[Categ]().gather[List]()

  def nextPosition: Fu[Int] =
    coll.primitiveOne[Int]($empty, $sort desc "pos", "pos") map (~_ + 1)

  def nbPosts(id: String): Fu[Int] =
    coll.primitiveOne[Int]($id(id), "nbPosts") map (~_)
}
