package lila.puzzle

import akka.stream.scaladsl._
import play.api.libs.json._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.config.MaxPerSecond
import lila.common.Json.jodaWrites
import lila.db.AsyncColl
import lila.db.dsl._
import lila.user.User

final class PuzzleActivity(
    puzzleColl: AsyncColl,
    roundColl: AsyncColl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import PuzzleActivity._
  import Round.RoundBSONHandler

  def stream(config: Config): Source[String, _] =
    Source futureSource {
      roundColl.map {
        _.find($doc("_id" $startsWith config.user.id))
          .sort($sort desc "_id")
          .batchSize(config.perSecond.value)
          .cursor[Round](ReadPreference.secondaryPreferred)
          .documentSource()
          .take(config.max | Int.MaxValue)
          .grouped(config.perSecond.value)
          .throttle(1, 1 second)
          .mapAsync(1)(enrich)
          .mapConcat(identity)
          .map { json =>
            s"${Json.stringify(json)}\n"
          }
      }
    }

  private def enrich(rounds: Seq[Round]): Fu[Seq[JsObject]] =
    puzzleColl {
      _.primitiveMap[Int, Double](
        ids = rounds.map(_.id.puzzleId),
        field = "perf.gl.r",
        fieldExtractor = obj =>
          for {
            perf   <- obj.child("perf")
            gl     <- perf.child("gl")
            rating <- gl.double("r")
          } yield rating
      ) map { ratings =>
        rounds flatMap { round =>
          ratings get round.id.puzzleId map { puzzleRating =>
            Json.obj(
              "id"           -> round.id.puzzleId,
              "date"         -> round.date,
              "rating"       -> round.rating,
              "ratingDiff"   -> round.ratingDiff,
              "puzzleRating" -> puzzleRating.toInt
            )
          }
        }
      }
    }
}

object PuzzleActivity {

  case class Config(
      user: User,
      max: Option[Int] = None,
      perSecond: MaxPerSecond
  )
}
