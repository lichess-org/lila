package lila.forum

import play.api.libs.json.Json

import lila.db.api._
import lila.db.Implicits._
import tube.categTube

object CategRepo {

  def bySlug(slug: String) = $find byId slug

  def withTeams(teams: List[String]): Fu[List[Categ]] = 
    $find($query($or(Seq(
      Json.obj("team" -> $exists(false)),
      Json.obj("team" -> $in(teams))
    ))) sort $sort.asc("pos"))

  def nextPosition: Fu[Int] = $primitive.one(
    $select.all,
    "pos",
    _ sort $sort.desc("pos")
  )(_.asOpt[Int]) map (~_ + 1)

  def nbPosts(id: String): Fu[Int] = 
    $primitive.one($select(id), "nbPosts")(_.asOpt[Int]) map (~_)
}
