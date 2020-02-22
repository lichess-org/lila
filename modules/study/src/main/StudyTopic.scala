package lila.study

import scala.concurrent.duration._
import reactivemongo.api._
import reactivemongo.api.bson._

import lila.db.dsl._
import lila.common.{ Future, WorkQueue }

case class StudyTopic(value: String) extends AnyVal

object StudyTopic {

  implicit val topicIso = lila.common.Iso.string[StudyTopic](StudyTopic.apply, _.value)
}

case class StudyTopics(value: List[StudyTopic]) extends AnyVal

object StudyTopics {

  val empty = StudyTopics(Nil)

  def fromStrs(strs: List[String]) = StudyTopics {
    strs.view
      .map(_.trim)
      .filter(t => t.size >= 2 && t.size <= 50)
      .take(30)
      .map(StudyTopic.apply)
      .toList
      .distinct
  }
}

final private class StudyTopicRepo(val coll: Coll)

final class StudyTopicApi(topicRepo: StudyTopicRepo, studyRepo: StudyRepo)(
    implicit ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {

  import BSONHandlers.StudyTopicBSONHandler

  def findLike(str: String, nb: Int = 10): Fu[List[StudyTopic]] =
    (str.size >= 2) ?? topicRepo.coll.ext
      .find($doc("_id".$startsWith(str, "i")))
      .sort($sort.naturalAsc)
      .list[Bdoc](nb.some, ReadPreference.secondaryPreferred)
      .dmap {
        _.flatMap(_.getAsOpt[StudyTopic]("_id"))
      }

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
