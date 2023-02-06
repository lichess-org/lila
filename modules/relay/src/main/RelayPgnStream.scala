package lila.relay

import akka.stream.scaladsl.*
import org.joda.time.format.DateTimeFormat

import lila.study.{ Chapter, ChapterRepo, PgnDump, Study, StudyRepo }
import lila.common.Bus
import chess.format.pgn.PgnStr

final class RelayPgnStream(
    roundRepo: RelayRoundRepo,
    studyRepo: StudyRepo,
    studyChapterRepo: ChapterRepo,
    studyPgnDump: PgnDump
)(using Executor):

  def exportFullTour(tour: RelayTour): Source[PgnStr, ?] =
    Source futureSource {
      roundRepo.idsByTourOrdered(tour) flatMap { ids =>
        studyRepo.byOrderedIds(StudyId.from[List, RelayRoundId](ids)) map { studies =>
          Source(studies).flatMapConcat { studyPgnDump(_, flags) }
        }
      }
    }

  private val flags = PgnDump.WithFlags(
    comments = false,
    variations = false,
    clocks = true,
    source = false,
    orientation = false
  )
  private val fileR      = """[\s,]""".r
  private val dateFormat = DateTimeFormat forPattern "yyyy.MM.dd"

  def filename(tour: RelayTour): String =
    val date = dateFormat.print(tour.syncedAt | tour.createdAt)
    fileR.replaceAllIn(s"lichess_broadcast_${tour.slug}_${tour.id}_$date", "")

  def streamRoundGames(rt: RelayRound.WithTourAndStudy): Source[PgnStr, ?] = {
    if (rt.relay.hasStarted) studyPgnDump(rt.study, flags)
    else Source.empty[PgnStr]
  } concat Source
    .queue[Set[StudyChapterId]](8, akka.stream.OverflowStrategy.dropHead)
    .mapMaterializedValue { queue =>
      val chan = SyncResult busChannel rt.relay.id
      val sub = Bus.subscribeFun(chan) { case SyncResult.Ok(chapters, _) =>
        queue
          .offer(chapters.view.filter(c => c.tagUpdate || c.newMoves > 0).map(_.id).toSet)
          .unit
      }
      queue.watchCompletion().addEffectAnyway {
        Bus.unsubscribe(sub, chan)
      }
    }
    .flatMapConcat(studyChapterRepo.byIdsSource)
    .throttle(16, 1 second)
    .mapAsync(1)(studyPgnDump.ofChapter(rt.study, flags))
