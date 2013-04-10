package lila.forum

import lila.db.Implicits._
import lila.db.api._
import tube.categTube

import play.api.libs.json.Json

object CategRepo {

  def bySlug(slug: String) = $find byId slug

  def withTeams(teams: List[String]): Fu[List[Categ]] = 
    $find($query($or(Json.obj(
      "team" -> $exists(false), 
      "team" -> $in(teams)
    ))) sort $sort.asc("pos"))

  def nextPosition: Fu[Int] = $primitive.one(
    $select.all,
    "pos",
    _ sort $sort.desc("pos")
  )(_.asOpt[Int]) map (~_ + 1)

  def nbPosts(id: String): Fu[Int] = 
    $primitive.one($select(id), "nbPosts")(_.asOpt[Int]) map (~_)
}
