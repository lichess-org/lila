package lila.forum

import lila.db.dsl._
import org.joda.time.DateTime

object PostRepo extends PostRepo(false) {

  def apply(troll: Boolean): PostRepo = troll.fold(PostRepoTroll, PostRepo)
}

object PostRepoTroll extends PostRepo(true)

sealed abstract class PostRepo(troll: Boolean) {

  import BSONHandlers.PostBSONHandler

  // dirty
  private val coll = Env.current.postColl

  private val trollFilter = troll.fold($empty, $doc("troll" -> false))

  def byCategAndId(categSlug: String, id: String): Fu[Option[Post]] =
    coll.one(selectCateg(categSlug) ++ $id(id))

  def countBeforeNumber(topicId: String, number: Int): Fu[Int] =
    coll.countSel(selectTopic(topicId) ++ $doc("number" -> $lt(number)))

  def isFirstPost(topicId: String, postId: String): Fu[Boolean] =
    coll.primitiveOne[String](selectTopic(topicId), $sort.createdAsc, "_id") map { _ contains postId }

  def countByTopics(topics: List[String]): Fu[Int] =
    coll.countSel(selectTopics(topics))

  def lastByTopics(topics: List[String]): Fu[Option[Post]] =
    coll.find(selectTopics(topics)).sort($sort.createdDesc).one[Post]

  def recentInCategs(nb: Int)(categIds: List[String], langs: List[String]): Fu[List[Post]] =
    coll.find(
      selectCategs(categIds) ++ selectLangs(langs) ++ selectNotHidden
    ).sort($sort.createdDesc).cursor[Post]().collect[List](nb)

  def removeByTopic(topicId: String): Funit =
    coll.remove(selectTopic(topicId)).void

  def hideByTopic(topicId: String, value: Boolean): Funit = coll.update(
    selectTopic(topicId),
    $doc("$set" -> $doc("hidden" -> value)),
    multi = true).void

  def selectTopic(topicId: String) = $doc("topicId" -> topicId) ++ trollFilter
  def selectTopics(topicIds: List[String]) = $doc("topicId" -> $in(topicIds)) ++ trollFilter

  def selectCateg(categId: String) = $doc("categId" -> categId) ++ trollFilter
  def selectCategs(categIds: List[String]) = $doc("categId" -> $in(categIds)) ++ trollFilter

  val selectNotHidden = $doc("hidden" -> false)

  def selectLangs(langs: List[String]) =
    if (langs.isEmpty) $empty
    else $doc("lang" $in langs)

  def findDuplicate(post: Post): Fu[Option[Post]] = coll.one($doc(
    "createdAt" $gt DateTime.now.minusHours(1),
    "userId" -> ~post.userId,
    "text" -> post.text
  ))

  def sortQuery = $sort.createdAsc

  def userIdsByTopicId(topicId: String): Fu[List[String]] =
    coll.distinct("userId", $doc("topicId" -> topicId).some) map lila.db.BSON.asStrings

  def idsByTopicId(topicId: String): Fu[List[String]] =
    coll.distinct("_id", $doc("topicId" -> topicId).some) map lila.db.BSON.asStrings

  import reactivemongo.api.ReadPreference
  def cursor(
    selector: Bdoc,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred) =
    coll.find(selector).cursor[Post](readPreference)
}
