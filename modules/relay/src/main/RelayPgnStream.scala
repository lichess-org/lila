package lila.relay

import akka.stream.scaladsl._
import org.joda.time.format.DateTimeFormat
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.study.{ PgnDump, Study, StudyRepo }

final class RelayPgnStream(
    roundRepo: RelayRoundRepo,
    studyRepo: StudyRepo,
    studyPgnDump: PgnDump
)(implicit ec: ExecutionContext) {

  def apply(tour: RelayTour): Source[String, _] =
    Source futureSource {
      roundRepo.idsByTourOrdered(tour) flatMap { ids =>
        studyRepo.byOrderedIds(ids.map(_.value).map(Study.Id)) map { studies =>
          Source(studies).flatMapConcat { studyPgnDump(_, flags) }
        }
      }
    }

  private val flags      = PgnDump.WithFlags(comments = false, variations = false, clocks = true)
  private val fileR      = """[\s,]""".r
  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd"

  def filename(tour: RelayTour): String = {
    val date = dateFormat.print(tour.syncedAt | tour.createdAt)
    fileR.replaceAllIn(s"lichess_broadcast_${tour.slug}_${tour.id}_$date", "")
  }
}
