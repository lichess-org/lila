package lila.relay

import akka.stream.scaladsl.*
import akka.stream.Materializer
import reactivemongo.akkastream.cursorProducer
import chess.format.pgn.{ Tag, PgnStr }

import lila.common.Bus
import lila.db.dsl.*
import lila.study.{ ChapterRepo, PgnDump, StudyRepo, Chapter }
import lila.relay.BSONHandlers.given

final class RelayPgnStream(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    studyRepo: StudyRepo,
    studyChapterRepo: ChapterRepo,
    studyPgnDump: PgnDump,
    baseUrl: lila.core.config.BaseUrl
)(using Executor, Materializer):

  def exportFullTourAs(tour: RelayTour, me: Option[User]): Source[PgnStr, ?] = Source.futureSource:
    for
      ids <- roundRepo.idsByTourOrdered(tour.id)
      studies <- studyRepo.byOrderedIds(StudyId.from[List, RelayRoundId](ids))
      visible = studies.filter(_.canView(me.map(_.id)))
    yield Source(visible)
      .flatMapConcat: study =>
        studyPgnDump.chaptersOf(study, _ => flags)
      .throttle(16, 1.second)

  private val flags = PgnDump.WithFlags(
    comments = true, // analysis
    variations = false,
    clocks = true,
    orientation = false
  )
  private def flagsFor(rt: RelayRound.WithTour, chapter: Chapter) = flags.copy(
    updateTags = _ + Tag("BroadcastName", rt.tour.name.value) +
      Tag("BroadcastURL", s"$baseUrl${rt.path}") +
      Tag("GameURL", s"$baseUrl${rt.path}/${chapter.id}")
  )
  private val fileR = """[\s,]""".r
  private val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")

  def filename(tour: RelayTour): String =
    val date = dateFormatter.print(tour.syncedAt | tour.createdAt)
    fileR.replaceAllIn(s"lichess_broadcast_${tour.slug}_${tour.id}_$date", "")

  def parseExportDate(id: RelayRoundId): Option[Instant] =
    val regex = """_(\d{4})_(\d{2})""".r
    id.value match
      case regex(y, m) =>
        for
          year <- y.toIntOption
          month <- m.toIntOption
          date <- scala.util.Try(instantOf(year, month, 1, 0, 0)).toOption
        yield date
      case _ => none

  def exportFullMonth(since: Instant): Source[PgnStr, ?] =
    roundRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(dateBetween("startedAt", since.some, since.plusMonths(1).some)),
          Sort(Ascending("startedAt")),
          PipelineOperator:
            $lookup.pipelineFull(
              from = tourRepo.coll.name,
              as = "tour",
              let = $doc("tourId" -> "$tourId"),
              pipe = List(
                $doc:
                  "$match" -> $expr:
                    $doc(
                      "$and" -> $arr(
                        $doc("$eq" -> $arr("$_id", "$$tourId")),
                        $doc("$gte" -> $arr("$tier", 3))
                      )
                    )
              )
            )
          ,
          Unwind("tour")
        )
      .documentSource()
      .mapConcat: doc =>
        for
          round <- doc.asOpt[RelayRound]
          tour <- doc.getAsOpt[RelayTour]("tour")
        yield round.withTour(tour)
      .mapAsync(4): rt =>
        studyRepo.publicById(rt.round.studyId).map2(rt -> _)
      .mapConcat(_.toList)
      .flatMapConcat: (rt, study) =>
        studyPgnDump.chaptersOf(study, c => flagsFor(rt, c))
      .throttle(100, 1.second)

  def streamRoundGames(rs: RelayRound.WithStudy): Source[PgnStr, ?] = {
    if rs.relay.hasStarted
    then studyPgnDump.chaptersOf(rs.study, _ => flags).throttle(32, 1.second)
    else Source.empty[PgnStr]
  }.concat(
    Source
      .queue[Set[StudyChapterId]](8, akka.stream.OverflowStrategy.dropHead)
      .mapMaterializedValue: queue =>
        val chan = SyncResult.busChannel(rs.relay.id)
        val sub = Bus.subscribeFunDyn(chan) { case SyncResult.Ok(chapters, _) =>
          queue.offer(chapters.view.filter(c => c.tagUpdate || c.newMoves > 0).map(_.id).toSet)
        }
        queue
          .watchCompletion()
          .addEffectAnyway:
            Bus.unsubscribeDyn(sub, List(chan))
      .flatMapConcat(studyChapterRepo.byIdsSource)
      .throttle(16, 1.second)
      .mapAsync(1)(studyPgnDump.ofChapter(rs.study, flags))
  )
