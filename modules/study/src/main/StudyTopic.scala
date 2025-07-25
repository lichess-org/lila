package lila.study

import play.api.libs.json.*
import reactivemongo.api.bson.*

import lila.common.LilaFuture
import lila.db.AsyncColl
import lila.db.dsl.{ *, given }

opaque type StudyTopic = String
object StudyTopic extends OpaqueString[StudyTopic]:

  val minLength = 2
  val maxLength = 50
  val broadcast: StudyTopic = "Broadcast"

  def fromStr(str: String): Option[StudyTopic] =
    str.trim match
      case s if s.lengthIs >= minLength && s.lengthIs <= maxLength => StudyTopic(s).some
      case _ => none

opaque type StudyTopics = List[StudyTopic]
object StudyTopics extends TotalWrapper[StudyTopics, List[StudyTopic]]:
  extension (e: StudyTopics)
    def diff(other: StudyTopics): StudyTopics = e.toSet.diff(other.value.toSet).toList
    def ++(other: StudyTopics): StudyTopics = e.toSet.++(other.value.toSet).toList

  val empty: StudyTopics = Nil
  val studyMax = 30
  val userMax = 128

  def fromStrs(strs: Seq[String], max: Int): StudyTopics =
    strs.view.flatMap(StudyTopic.fromStr).take(max).toList.distinct

final private class StudyTopicRepo(val coll: AsyncColl)
final private class StudyUserTopicRepo(val coll: AsyncColl)

final class StudyTopicApi(topicRepo: StudyTopicRepo, userTopicRepo: StudyUserTopicRepo, studyRepo: StudyRepo)(
    using
    Executor,
    Scheduler
):

  def byId(str: String): Fu[Option[StudyTopic]] =
    topicRepo.coll(_.byId[Bdoc](str)).dmap { _.flatMap(docTopic) }

  def findLike(str: String, myId: Option[UserId], nb: Int = 10): Fu[StudyTopics] = StudyTopics.from:
    (str.lengthIs >= 2).so:
      val favsFu: Fu[List[StudyTopic]] =
        myId.so: userId =>
          userTopics(userId).map:
            _.value.filter(_.value.startsWith(str)).take(nb)
      favsFu.flatMap: favs =>
        topicRepo
          .coll:
            _.find($doc("_id".$startsWith(java.util.regex.Pattern.quote(str), "i")))
              .sort($sort.naturalAsc)
              .cursor[Bdoc]()
              .list(nb - favs.size)
          .dmap { _.flatMap(docTopic) }
          .dmap { favs ::: _ }

  def userTopics(userId: UserId): Fu[StudyTopics] =
    userTopicRepo.coll:
      _.primitiveOne[List[StudyTopic]]($id(userId), "topics")
        .dmap(_.fold(StudyTopics.empty)(StudyTopics(_)))

  private case class TagifyTopic(value: String)
  private given Reads[TagifyTopic] = Json.reads

  def userTopics(user: User, json: String): Funit =
    val topics =
      if json.trim.isEmpty then StudyTopics.empty
      else
        Json.parse(json).validate[List[TagifyTopic]] match
          case JsSuccess(topics, _) => StudyTopics.fromStrs(topics.map(_.value), StudyTopics.userMax)
          case _ => StudyTopics.empty
    userTopicRepo
      .coll:
        _.update.one(
          $id(user.id),
          $set("topics" -> topics),
          upsert = true
        )
      .void

  def userTopicsAdd(userId: UserId, topics: StudyTopics): Funit =
    topics.value.nonEmpty.so(userTopics(userId).flatMap { prev =>
      val newTopics = prev ++ topics
      (newTopics != prev).so(
        userTopicRepo
          .coll:
            _.update.one($id(userId), $set("topics" -> newTopics), upsert = true)
          .void
      )
    })

  def userTopicsDelete(userId: UserId) =
    userTopicRepo.coll(_.delete.one($id(userId)))

  def popular(nb: Int): Fu[StudyTopics] =
    StudyTopics.from(
      topicRepo
        .coll:
          _.find($empty)
            .sort($sort.naturalAsc)
            .cursor[Bdoc]()
            .list(nb)
        .dmap:
          _.flatMap(docTopic)
    )

  private def docTopic(doc: Bdoc): Option[StudyTopic] =
    doc.getAsOpt[StudyTopic]("_id")

  private val recomputeWorkQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(1),
    timeout = 61.seconds,
    name = "studyTopicAggregation",
    lila.log.asyncActorMonitor.unhandled
  )

  def recompute(): Unit =
    recomputeWorkQueue(LilaFuture.makeItLast(60.seconds)(recomputeNow)).recover:
      case _: scalalib.actor.AsyncActorBounded.EnqueueException => ()
      case e: Exception => logger.warn("Can't recompute study topics!", e)

  private def recomputeNow: Funit =
    studyRepo
      .coll:
        _.aggregateWith[Bdoc](): framework =>
          import framework.*
          List(
            Match(
              $doc(
                "topics".$exists(true),
                "visibility" -> "public"
              )
            ),
            Project($doc("topics" -> true, "_id" -> false)),
            UnwindField("topics"),
            SortByFieldCount("topics"),
            Project($doc("_id" -> true)),
            Out(topicRepo.coll.name.value)
          )
        .headOption
      .void
