package lila.study

import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._
import play.api.libs.json._

import lila.common.{ Future, WorkQueue }
import lila.db.dsl._
import lila.user.User

case class StudyTopic(value: String) extends AnyVal with StringValue

object StudyTopic {

  val minLength = 2
  val maxLength = 50

  def fromStr(str: String): Option[StudyTopic] = str.trim match {
    case s if s.size >= minLength && s.size <= maxLength => StudyTopic(s).some
    case _                                               => none
  }

  implicit val topicIso = lila.common.Iso.string[StudyTopic](StudyTopic.apply, _.value)
}

case class StudyTopics(value: List[StudyTopic]) extends AnyVal

object StudyTopics {

  val empty = StudyTopics(Nil)

  def fromStrs(strs: Seq[String]) = StudyTopics {
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
    implicit ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {

  import BSONHandlers.{ StudyTopicBSONHandler, StudyTopicsBSONHandler }

  def byId(str: String): Fu[Option[StudyTopic]] =
    topicRepo.coll.byId[Bdoc](str) dmap { _ flatMap docTopic }

  def findLike(str: String, nb: Int = 10): Fu[StudyTopics] = {
    (str.size >= 2) ?? topicRepo.coll.ext
      .find($doc("_id".$startsWith(str, "i")))
      .sort($sort.naturalAsc)
      .list[Bdoc](nb.some, ReadPreference.secondaryPreferred)
      .dmap {
        _ flatMap docTopic
      }
  } dmap StudyTopics.apply

  def userTopics(user: User): Fu[StudyTopics] =
    userTopicRepo.coll.byId(user.id).map {
      _.flatMap(_.getAsOpt[StudyTopics]("topics")) | StudyTopics.empty
    }

  private case class TagifyTopic(value: String)
  implicit private val TagifyTopicReads = Json.reads[TagifyTopic]
  def userTopics(user: User, json: String): Funit = {
    val topics = Json.parse(json).validate[List[TagifyTopic]] match {
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

  def popular(nb: Int): Fu[StudyTopics] =
    topicRepo.coll.ext
      .find($empty)
      .sort($sort.naturalAsc)
      .list[Bdoc](nb.some, ReadPreference.secondaryPreferred)
      .dmap {
        _ flatMap docTopic
      }
      .dmap(StudyTopics.apply)

  private def docTopic(doc: Bdoc): Option[StudyTopic] =
    doc.getAsOpt[StudyTopic]("_id")

  private val recomputeWorkQueue = new WorkQueue(
    buffer = 1,
    timeout = 61 seconds,
    name = "studyTopicAggregation",
    parallelism = 1
  )

  def recompute(): Unit =
    recomputeWorkQueue(Future.makeItLast(60 seconds)(recomputeNow)).recover {
      case _: WorkQueue.EnqueueException => ()
      case e: Exception                  => logger.warn("Can't recompute study topics!", e)
    }

  private def recomputeNow: Funit =
    studyRepo.coll
      .aggregateWith[Bdoc]() { framework =>
        import framework._
        Match("topics" $exists true) -> List(
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
