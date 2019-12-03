package lila.forum

import org.joda.time.DateTime
import reactivemongo.api.{ CursorProducer, ReadPreference }

import lila.db.dsl._
import lila.user.User

private final class PostRepo(val coll: Coll, troll: Boolean = false) {

  def withTroll(t: Boolean) = if (t == troll) this else new PostRepo(coll, t)

  import BSONHandlers.PostBSONHandler

  private val trollFilter = !troll ?? $doc("troll" -> false)

  def byIds(ids: List[Post.ID]) = coll.byIds[Post](ids)

  def byCategAndId(categSlug: String, id: String): Fu[Option[Post]] =
    coll.uno[Post](selectCateg(categSlug) ++ $id(id))

  def countBeforeNumber(topicId: String, number: Int): Fu[Int] =
    coll.countSel(selectTopic(topicId) ++ $doc("number" -> $lt(number)))

  def isFirstPost(topicId: String, postId: String): Fu[Boolean] =
    coll.primitiveOne[String](selectTopic(topicId), $sort.createdAsc, "_id") map { _ contains postId }

  def countByTopic(topic: Topic): Fu[Int] =
    coll.countSel(selectTopic(topic.id))

  def lastByCateg(categ: Categ): Fu[Option[Post]] =
    coll.ext.find(selectCateg(categ.id)).sort($sort.createdDesc).uno[Post]

  def lastByTopic(topic: Topic): Fu[Option[Post]] =
    coll.ext.find(selectTopic(topic.id)).sort($sort.createdDesc).uno[Post]

  def recentInCategs(nb: Int)(categIds: List[String], langs: List[String]): Fu[List[Post]] =
    coll.ext.find(
      selectCategs(categIds) ++ selectLangs(langs) ++ selectNotHidden
    ).sort($sort.createdDesc).cursor[Post]().gather[List](nb)

  def countByCateg(categ: Categ): Fu[Int] =
    coll.countSel(selectCateg(categ.id))

  def removeByTopic(topicId: String): Funit =
    coll.delete.one(selectTopic(topicId)).void

  def hideByTopic(topicId: String, value: Boolean): Funit = coll.update.one(
    selectTopic(topicId),
    $set("hidden" -> value),
    multi = true
  ).void

  def selectTopic(topicId: String) = $doc("topicId" -> topicId) ++ trollFilter

  def selectCateg(categId: String) = $doc("categId" -> categId) ++ trollFilter
  def selectCategs(categIds: List[String]) = $doc("categId" $in categIds) ++ trollFilter

  val selectNotHidden = $doc("hidden" -> false)

  def selectLangs(langs: List[String]) =
    if (langs.isEmpty) $empty
    else $doc("lang" $in langs)

  def findDuplicate(post: Post): Fu[Option[Post]] = coll.uno[Post]($doc(
    "createdAt" $gt DateTime.now.minusHours(1),
    "userId" -> ~post.userId,
    "text" -> post.text
  ))

  def sortQuery = $sort.createdAsc

  def userIdsByTopicId(topicId: String): Fu[List[String]] =
    coll.distinctEasy[User.ID, List]("userId", $doc("topicId" -> topicId))

  def idsByTopicId(topicId: String): Fu[List[String]] =
    coll.distinctEasy[String, List]("_id", $doc("topicId" -> topicId))
}
