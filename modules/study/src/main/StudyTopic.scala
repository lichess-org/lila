package lila.study

import play.api.libs.json.*
import reactivemongo.api.bson.*
import scala.concurrent.duration.*

import lila.common.Future
import lila.db.AsyncColl
import lila.db.dsl.{ *, given }
import lila.user.User
import lila.common.Iso
import lila.common.config.Max

opaque type StudyTopic = String
object StudyTopic extends OpaqueString[StudyTopic]:

  val minLength = 2
  val maxLength = 50

  def fromStr(str: String): Option[StudyTopic] =
    str.trim match
      case s if s.lengthIs >= minLength && s.lengthIs <= maxLength => StudyTopic(s).some
      case _                                                       => none

case class StudyTopics(value: List[StudyTopic]) extends AnyVal:

  def diff(other: StudyTopics) =
    StudyTopics {
      value.toSet.diff(other.value.toSet).toList
    }

  def ++(other: StudyTopics) =
    StudyTopics {
      value.toSet.++(other.value.toSet).toList
    }

object StudyTopics:

  val studyMax = 30
  val userMax  = 128

  val empty = StudyTopics(Nil)

  def fromStrs(strs: Seq[String], max: Int) =
    StudyTopics {
      strs.view
        .flatMap(StudyTopic.fromStr)
        .take(max)
        .toList
        .distinct
    }

final private class StudyTopicRepo(val coll: AsyncColl)
final private class StudyUserTopicRepo(val coll: AsyncColl)

final class StudyTopicApi(topicRepo: StudyTopicRepo, userTopicRepo: StudyUserTopicRepo, studyRepo: StudyRepo)(
    implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler
):

  import BSONHandlers.given

  def byId(str: String): Fu[Option[StudyTopic]] =
    topicRepo.coll(_.byId[Bdoc](str)) dmap { _ flatMap docTopic }

  def findLike(str: String, myId: Option[UserId], nb: Int = 10): Fu[StudyTopics] = {
    (str.lengthIs >= 2) ?? {
      val favsFu: Fu[List[StudyTopic]] =
        myId.?? { userId =>
          userTopics(userId).map {
            _.value.filter(_.value startsWith str) take nb
          }
        }
      favsFu flatMap { favs =>
        topicRepo
          .coll {
            _.find($doc("_id".$startsWith(java.util.regex.Pattern.quote(str), "i")))
              .sort($sort.naturalAsc)
              .cursor[Bdoc](readPref)
              .list(nb - favs.size)
          }
          .dmap { _ flatMap docTopic }
          .dmap { favs ::: _ }
      }
    }
  } dmap StudyTopics.apply

  def userTopics(userId: UserId): Fu[StudyTopics] =
    userTopicRepo.coll {
      _.primitiveOne[List[StudyTopic]]($id(userId), "topics")
        .dmap(_.fold(StudyTopics.empty)(StudyTopics.apply))
    }

  private case class TagifyTopic(value: String)
  private given Reads[TagifyTopic] = Json.reads

  def userTopics(user: User, json: String): Funit =
    val topics =
      if (json.trim.isEmpty) StudyTopics.empty
      else
        Json.parse(json).validate[List[TagifyTopic]] match
          case JsSuccess(topics, _) => StudyTopics.fromStrs(topics.map(_.value), StudyTopics.userMax)
          case _                    => StudyTopics.empty
    userTopicRepo.coll {
      _.update.one(
        $id(user.id),
        $set("topics" -> topics),
        upsert = true
      )
    }.void

  def userTopicsAdd(userId: UserId, topics: StudyTopics): Funit =
    topics.value.nonEmpty ?? userTopics(userId).flatMap { prev =>
      val newTopics = prev ++ topics
      (newTopics != prev) ??
        userTopicRepo.coll {
          _.update.one($id(userId), $set("topics" -> newTopics), upsert = true)
        }.void
    }

  def popular(nb: Int): Fu[StudyTopics] =
    topicRepo
      .coll {
        _.find($empty)
          .sort($sort.naturalAsc)
          .cursor[Bdoc]()
          .list(nb)
      }
      .dmap {
        _ flatMap docTopic
      }
      .dmap(StudyTopics.apply)

  private def docTopic(doc: Bdoc): Option[StudyTopic] =
    doc.getAsOpt[StudyTopic]("_id")

  private val recomputeWorkQueue = lila.hub.AsyncActorSequencer(
    maxSize = Max(1),
    timeout = 61 seconds,
    name = "studyTopicAggregation",
    logging = false
  )

  def recompute(): Unit =
    recomputeWorkQueue(Future.makeItLast(60 seconds)(recomputeNow)).recover {
      case _: lila.hub.BoundedAsyncActor.EnqueueException => ()
      case e: Exception                                   => logger.warn("Can't recompute study topics!", e)
    }.unit

  private def recomputeNow: Funit =
    studyRepo.coll {
      _.aggregateWith[Bdoc]() { framework =>
        import framework.*
        List(
          Match(
            $doc(
              "topics" $exists true,
              "visibility" -> "public"
            )
          ),
          Project($doc("topics" -> true, "_id" -> false)),
          UnwindField("topics"),
          SortByFieldCount("topics"),
          Project($doc("_id" -> true)),
          Out(topicRepo.coll.name.value)
        )
      }.headOption
    }.void
