package lila.study

import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._
import play.api.libs.json._

import lila.common.Future
import lila.db.dsl._
import lila.user.User

case class StudyTopic(value: String) extends AnyVal with StringValue

object StudyTopic {

  val minLength = 2
  val maxLength = 50

  def fromStr(str: String): Option[StudyTopic] =
    str.trim match {
      case s if s.lengthIs >= minLength && s.lengthIs <= maxLength => StudyTopic(s).some
      case _                                                       => none
    }

  implicit val topicIso = lila.common.Iso.string[StudyTopic](StudyTopic.apply, _.value)
}

case class StudyTopics(value: List[StudyTopic]) extends AnyVal {

  def diff(other: StudyTopics) =
    StudyTopics {
      value.toSet.diff(other.value.toSet).toList
    }

  def ++(other: StudyTopics) =
    StudyTopics {
      value.toSet.++(other.value.toSet).toList
    }
}

object StudyTopics {

  val empty = StudyTopics(Nil)

  def fromStrs(strs: Seq[String]) =
    StudyTopics {
      strs.view
        .flatMap(StudyTopic.fromStr)
        .take(30)
        .toList
        .distinct
    }
}

final private class StudyTopicRepo(val coll: Coll)
final private class StudyUserTopicRepo(val coll: Coll)

final class StudyTopicApi(topicRepo: StudyTopicRepo, userTopicRepo: StudyUserTopicRepo, studyRepo: StudyRepo)(
    implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import BSONHandlers.{ StudyTopicBSONHandler, StudyTopicsBSONHandler }

  def byId(str: String): Fu[Option[StudyTopic]] =
    topicRepo.coll.byId[Bdoc](str) dmap { _ flatMap docTopic }

  def findLike(str: String, myId: Option[User.ID], nb: Int = 10): Fu[StudyTopics] = {
    (str.lengthIs >= 2) ?? {
      val favsFu: Fu[List[StudyTopic]] =
        myId.?? { userId =>
          userTopics(userId).map {
            _.value.filter(_.value startsWith str) take nb
          }
        }
      favsFu flatMap { favs =>
        topicRepo.coll
          .find($doc("_id".$startsWith(str, "i")))
          .sort($sort.naturalAsc)
          .cursor[Bdoc](ReadPreference.secondaryPreferred)
          .list(nb - favs.size)
          .dmap {
            _ flatMap docTopic
          }
          .dmap { favs ::: _ }
      }
    }
  } dmap StudyTopics.apply

  def userTopics(userId: User.ID): Fu[StudyTopics] =
    userTopicRepo.coll.byId(userId).map {
      _.flatMap(_.getAsOpt[StudyTopics]("topics")) | StudyTopics.empty
    }

  private case class TagifyTopic(value: String)
  implicit private val TagifyTopicReads = Json.reads[TagifyTopic]

  def userTopics(user: User, json: String): Funit = {
    val topics =
      if (json.trim.isEmpty) StudyTopics.empty
      else
        Json.parse(json).validate[List[TagifyTopic]] match {
          case JsSuccess(topics, _) => StudyTopics fromStrs topics.map(_.value)
          case _                    => StudyTopics.empty
        }
    userTopicRepo.coll.update
      .one(
        $id(user.id),
        $set("topics" -> topics),
        upsert = true
      )
      .void
  }

  def userTopicsAdd(userId: User.ID, topics: StudyTopics): Funit =
    topics.value.nonEmpty ??
      userTopicRepo.coll.update
        .one(
          $id(userId),
          $addToSet("topics" -> $doc("$each" -> topics)),
          upsert = true
        )
        .void

  def popular(nb: Int): Fu[StudyTopics] =
    topicRepo.coll
      .find($empty)
      .sort($sort.naturalAsc)
      .cursor[Bdoc]()
      .list(nb)
      .dmap {
        _ flatMap docTopic
      }
      .dmap(StudyTopics.apply)

  private def docTopic(doc: Bdoc): Option[StudyTopic] =
    doc.getAsOpt[StudyTopic]("_id")

  private val recomputeWorkQueue = new lila.hub.DuctSequencer(
    maxSize = 1,
    timeout = 61 seconds,
    name = "studyTopicAggregation",
    logging = false
  )

  def recompute(): Unit =
    recomputeWorkQueue(Future.makeItLast(60 seconds)(recomputeNow)).recover {
      case _: lila.hub.BoundedDuct.EnqueueException => ()
      case e: Exception                             => logger.warn("Can't recompute study topics!", e)
    }

  private def recomputeNow: Funit =
    studyRepo.coll
      .aggregateWith[Bdoc]() { framework =>
        import framework._
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
          Out(topicRepo.coll.name)
        )
      }
      .headOption
      .void
}
