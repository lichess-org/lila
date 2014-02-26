package lila.forum

import play.api.libs.json.Json

import lila.db.api._
import lila.db.Implicits._
import tube.postTube

object PostRepo extends PostRepo(false) {

  def apply(troll: Boolean): PostRepo = troll.fold(PostRepoTroll, PostRepo)
}

object PostRepoTroll extends PostRepo(true)

sealed abstract class PostRepo(troll: Boolean) {

  private lazy val trollFilter = troll.fold(
    Json.obj(),
    Json.obj("troll" -> false)
  )

  def byCategAndId(categSlug: String, id: String): Fu[Option[Post]] =
    $find.one(selectCateg(categSlug) ++ $select(id))

  def countBeforeNumber(topicId: String, number: Int): Fu[Int] =
    $count(selectTopic(topicId) ++ Json.obj("number" -> $lt(number)))

  def isFirstPost(topicId: String, postId: String): Fu[Boolean] =
    $primitive.one(
      selectTopic(topicId),
      "_id",
      _ sort $sort.createdAsc
    )(_.asOpt[String]) map { _.??(postId ==) }

  def countByTopics(topics: List[String]): Fu[Int] =
    $count(selectTopics(topics))

  def lastByTopics(topics: List[String]): Fu[Option[Post]] =
    $find.one($query(selectTopics(topics)) sort $sort.createdDesc)

  def recentInCategs(nb: Int)(categIds: List[String], langs: List[String]): Fu[List[Post]] =
    $find($query(selectCategs(categIds) ++ selectLangs(langs) pp) sort $sort.createdDesc, nb)

  def removeByTopic(topicId: String): Fu[Unit] =
    $remove(selectTopic(topicId))

  def selectTopic(topicId: String) = Json.obj("topicId" -> topicId) ++ trollFilter
  def selectTopics(topicIds: List[String]) = Json.obj("topicId" -> $in(topicIds)) ++ trollFilter

  def selectCateg(categId: String) = Json.obj("categId" -> categId) ++ trollFilter
  def selectCategs(categIds: List[String]) = Json.obj("categId" -> $in(categIds)) ++ trollFilter

  def selectLangs(langs: List[String]) = Json.obj("lang" -> $in(langs))

  def sortQuery = $sort.createdAsc
}
