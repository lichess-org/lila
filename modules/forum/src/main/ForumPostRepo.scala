package lila.forum

import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }

import lila.core.forum.ForumPostMini
import lila.db.dsl.{ *, given }
import lila.forum.Filter.*

final class ForumPostRepo(val coll: Coll, filter: Filter = Safe)(using Executor):

  def forUser(user: Option[User]) =
    withFilter(user.filter(_.marks.troll).fold[Filter](Safe) { u =>
      SafeAnd(u.id)
    })
  def withFilter(f: Filter) = if f == filter then this else new ForumPostRepo(coll, f)
  def unsafe                = withFilter(Unsafe)

  import BSONHandlers.given

  private val noTroll = $doc("troll" -> false)
  private val trollFilter = filter match
    case Safe       => noTroll
    case SafeAnd(u) => $or(noTroll, $doc("userId" -> u))
    case Unsafe     => $empty

  private val miniProjection = $doc(
    "topicId"   -> true,
    "userId"    -> true,
    "text"      -> true,
    "troll"     -> true,
    "createdAt" -> true
  )

  def miniByIds(ids: Seq[ForumPostId]) =
    coll.byOrderedIds[ForumPostMini, ForumPostId](ids, miniProjection.some)(_.id)

  def countBeforeNumber(topicId: ForumTopicId, number: Int): Fu[Int] =
    coll.countSel(selectTopic(topicId) ++ $doc("number" -> $lt(number)))

  def isFirstPost(topicId: ForumTopicId, postId: ForumPostId): Fu[Boolean] =
    coll.primitiveOne[String](selectTopic(topicId), $sort.createdAsc, "_id").dmap { _ contains postId }

  def countByTopic(topic: ForumTopic): Fu[Int] =
    coll.countSel(selectTopic(topic.id))

  def lastByCateg(categ: ForumCateg): Fu[Option[ForumPost]] =
    coll.find(selectCateg(categ.id)).sort($sort.createdDesc).one[ForumPost]

  def lastByTopic(topic: ForumTopic): Fu[Option[ForumPost]] =
    coll.find(selectTopic(topic.id)).sort($sort.createdDesc).one[ForumPost]

  def recentInCategs(nb: Int)(categIds: List[ForumCategId], langs: List[String]): Fu[List[ForumPost]] =
    coll
      .find(selectCategs(categIds) ++ selectLangs(langs) ++ selectNotErased)
      .sort($sort.createdDesc)
      .cursor[ForumPost]()
      .list(nb)

  def recentIdsInCateg(categId: ForumCategId, nb: Int): Fu[List[ForumPostId]] =
    coll
      .find(selectCateg(categId) ++ selectNotErased, $id(true).some)
      .sort($sort.createdDesc)
      .cursor[Bdoc]()
      .list(nb)
      .map:
        _.flatMap:
          _.getAsOpt[ForumPostId]("_id")

  def allByUserCursor(user: User): AkkaStreamCursor[ForumPost] =
    coll
      .find($doc("userId" -> user.id))
      .cursor[ForumPost](ReadPref.priTemp)

  def countByCateg(categ: ForumCateg): Fu[Int] =
    coll.countSel(selectCateg(categ.id))

  def remove(post: ForumPost): Funit =
    coll.delete.one($id(post.id)).void

  def removeByTopic(topicId: ForumTopicId): Funit =
    coll.delete.one(selectTopic(topicId)).void

  def selectTopic(topicId: ForumTopicId) = $doc("topicId" -> topicId) ++ trollFilter

  def selectCateg(categId: ForumCategId)         = $doc("categId" -> categId) ++ trollFilter
  def selectCategs(categIds: List[ForumCategId]) = $doc("categId".$in(categIds)) ++ trollFilter

  val selectNotErased = $doc("erasedAt".$exists(false))

  def selectLangs(langs: List[String]) =
    if langs.isEmpty then $empty
    else $doc("lang".$in(langs))

  def findDuplicate(post: ForumPost): Fu[Option[ForumPost]] =
    coll.one[ForumPost](
      $doc(
        "createdAt".$gt(nowInstant.minusHours(1)),
        "userId" -> post.userId,
        "text"   -> post.text
      )
    )

  def idsByTopicId(topicId: ForumTopicId): Fu[List[ForumPostId]] =
    coll.distinctEasy[ForumPostId, List]("_id", $doc("topicId" -> topicId), _.sec)

  def allUserIdsByTopicId(topicId: ForumTopicId): Fu[List[UserId]] =
    coll.distinctEasy[UserId, List](
      "userId",
      $doc("topicId" -> topicId) ++ selectNotErased,
      _.sec
    )

  def eraseAllBy(id: UserId) =
    coll.update.one(
      $doc("userId" -> id),
      $set($doc("userId" -> UserId.ghost, "text" -> "", "erasedAt" -> nowInstant)),
      multi = true
    )

  private[forum] def nonGhostCursor(since: Option[Instant]): AkkaStreamCursor[ForumPostMini] =
    val noGhost = $doc("userId".$ne(UserId.ghost))
    val filter  = since.fold(noGhost)(instant => $and(noGhost, $doc("createdAt".$gt(instant))))
    coll
      .find(filter, miniProjection.some)
      .cursor[ForumPostMini](ReadPref.priTemp)
