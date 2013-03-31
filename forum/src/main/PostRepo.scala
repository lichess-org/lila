package lila.forum

import lila.db.Implicits._
import lila.db.api._

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

object PostRepo {

  private[forum] implicit def tube = postTube

  def isFirstPost(topicId: String, postId: String): Fu[Boolean] =
    $primitive.one(
      selectTopic(topicId),
      "_id",
      _ sort $sort.createdAsc
    )(_.asOpt[String]) map { _.zmap(postId ==) }

  def countByTopics(topics: List[String]): Fu[Int] =
    $count(selectTopics(topics))

  def lastByTopics(topics: List[String]): Fu[Option[Post]] =
    $find.one($query(selectTopics(topics)) sort $sort.createdDesc)

  def recentInCategs(nb: Int)(categIds: List[String]): Fu[List[Post]] =
    $find($query(selectCategs(categIds)) sort $sort.createdDesc limit nb)

  def removeByTopic(topicId: String): Fu[Unit] =
    $remove(selectTopic(topicId))

  def selectTopic(topicId: String) = Json.obj("topicId" -> topicId)
  def selectTopics(topicIds: List[String]) = Json.obj("topicId" -> $in(topicIds))

  def selectCategs(categIds: List[String]) = Json.obj("categId" -> $in(categIds))

  def sortQuery = $sort.createdDesc
}
