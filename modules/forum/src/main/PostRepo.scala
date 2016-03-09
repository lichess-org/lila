package lila.forum

import play.api.libs.json.Json

import lila.db.api._
import lila.db.Implicits._
import org.joda.time.DateTime
import reactivemongo.bson.BSONDocument
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
    $find($query(
      selectCategs(categIds) ++ selectLangs(langs) ++ selectNotHidden
    ) sort $sort.createdDesc, nb)

  def removeByTopic(topicId: String): Fu[Unit] =
    $remove(selectTopic(topicId))

  def hideByTopic(topicId: String, value: Boolean): Fu[Unit] = $update(
    selectTopic(topicId),
    BSONDocument("$set" -> BSONDocument("hidden" -> value)),
    multi = true)

  def selectTopic(topicId: String) = Json.obj("topicId" -> topicId) ++ trollFilter
  def selectTopics(topicIds: List[String]) = Json.obj("topicId" -> $in(topicIds)) ++ trollFilter

  def selectCateg(categId: String) = Json.obj("categId" -> categId) ++ trollFilter
  def selectCategs(categIds: List[String]) = Json.obj("categId" -> $in(categIds)) ++ trollFilter

  val selectNotHidden = Json.obj("hidden" -> false)

  def selectLangs(langs: List[String]) =
    if (langs.isEmpty) Json.obj()
    else Json.obj("lang" -> $in(langs))

  def findDuplicate(post: Post): Fu[Option[Post]] = $find.one(Json.obj(
    "createdAt" -> $gt($date(DateTime.now.minusHours(1))),
    "userId" -> ~post.userId,
    "text" -> post.text
  ))

  def sortQuery = $sort.createdAsc

  def userIdsByTopicId(topicId: String): Fu[List[String]] =
    postTube.coll.distinct[String, List]("userId", BSONDocument("topicId" -> topicId).some)

  def idsByTopicId(topicId: String): Fu[List[String]] =
    postTube.coll.distinct[String, List]("_id", BSONDocument("topicId" -> topicId).some)
}
