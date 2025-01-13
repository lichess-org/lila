package lila.relay

import akka.stream.scaladsl.*
import chess.format.pgn.PgnStr

import lila.common.Bus
import lila.study.{ ChapterRepo, PgnDump, StudyRepo }

final class RelayPgnStream(
    roundRepo: RelayRoundRepo,
    studyRepo: StudyRepo,
    studyChapterRepo: ChapterRepo,
    studyPgnDump: PgnDump
)(using Executor):

  def exportFullTourAs(tour: RelayTour, me: Option[User]): Source[PgnStr, ?] = Source.futureSource:
    for
      ids     <- roundRepo.idsByTourOrdered(tour.id)
      studies <- studyRepo.byOrderedIds(StudyId.from[List, RelayRoundId](ids))
      visible = studies.filter(_.canView(me.map(_.id)))
    yield Source(visible).flatMapConcat { studyPgnDump.chaptersOf(_, flags) }.throttle(16, 1.second)

  private val flags = PgnDump.WithFlags(
    comments = false,
    variations = false,
    clocks = true,
    source = false,
    orientation = false,
    site = none
  )
  private val fileR         = """[\s,]""".r
  private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")

  def filename(tour: RelayTour): String =
    val date = dateFormatter.print(tour.syncedAt | tour.createdAt)
    fileR.replaceAllIn(s"lichess_broadcast_${tour.slug}_${tour.id}_$date", "")

  def streamRoundGames(rs: RelayRound.WithStudy): Source[PgnStr, ?] = {
    if rs.relay.hasStarted then studyPgnDump.chaptersOf(rs.study, flags).throttle(32, 1.second)
    else Source.empty[PgnStr]
  }.concat(
    Source
      .queue[Set[StudyChapterId]](8, akka.stream.OverflowStrategy.dropHead)
      .mapMaterializedValue: queue =>
        val chan = SyncResult.busChannel(rs.relay.id)
        val sub = Bus.subscribeFun(chan) { case SyncResult.Ok(chapters, _) =>
          queue.offer(chapters.view.filter(c => c.tagUpdate || c.newMoves > 0).map(_.id).toSet)
        }
        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsubscribe(sub, chan)
      .flatMapConcat(studyChapterRepo.byIdsSource)
      .throttle(16, 1.second)
      .mapAsync(1)(studyPgnDump.ofChapter(rs.study, flags))
  )
