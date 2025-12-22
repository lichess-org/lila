package lila.relay

import akka.stream.scaladsl.*
import akka.stream.Materializer
import reactivemongo.akkastream.cursorProducer
import chess.format.pgn.{ Tag, PgnStr }

import lila.common.Bus
import lila.db.dsl.*
import lila.study.{ ChapterRepo, PgnDump, StudyRepo, Study, Chapter }
import lila.relay.BSONHandlers.given

final class RelayPgnStream(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    studyRepo: StudyRepo,
    studyChapterRepo: ChapterRepo,
    studyPgnDump: PgnDump,
    routeUrl: lila.core.config.RouteUrl
)(using Executor, Materializer):

  def ofGame(rt: RelayRound.WithTourAndStudy, chapter: Chapter): Fu[PgnStr] =
    studyPgnDump.ofChapter(rt.study, flagsFor(rt.withTour, chapter))(chapter)

  def ofGames(rt: RelayRound.WithTourAndStudy): Source[PgnStr, ?] =
    studyPgnDump.chaptersOf(rt.study, flagsFor(rt.withTour, _))

  def ofChapter(sc: Study.WithChapter): Fu[Option[PgnStr]] =
    roundRepo
      .byIdWithTour(sc.study.id.into(RelayRoundId))
      .flatMapz: rt =>
        ofGame(rt.withStudy(sc.study), sc.chapter).map(_.some)

  def ofStudy(s: Study): Fu[Option[Source[PgnStr, ?]]] =
    roundRepo
      .byIdWithTour(s.id.into(RelayRoundId))
      .map2: rt =>
        ofGames(rt.withStudy(s))

  def ofFirstChapter(s: Study): Fu[Option[PgnStr]] =
    studyChapterRepo
      .firstByStudy(s.id)
      .flatMapz: chapter =>
        ofChapter(Study.WithChapter(s, chapter))

  def exportFullTourAs(tour: RelayTour, me: Option[User]): Source[PgnStr, ?] = Source.futureSource:
    for
      rounds <- roundRepo.byTourOrdered(tour.id)
      studies <- studyRepo.byOrderedIds(rounds.map(_.studyId))
      visible = studies.filter(_.canView(me.map(_.id)))
      withStudy =
        for
          r <- rounds
          s <- visible.find(_.id == r.studyId)
        yield r.withTour(tour).withStudy(s)
    yield Source(withStudy).flatMapConcat(ofGames).throttle(20, 1.second)

  private val baseFlags = PgnDump.WithFlags(
    comments = true, // analysis
    variations = false,
    clocks = true,
    orientation = false
  )
  private def flagsFor(rt: RelayRound.WithTour, chapter: Chapter) =
    baseFlags.copy(
      updateTags = tags =>
        val gameUrl = routeUrl(rt.call(chapter.id))
        val site = tags(_.Site)
          .flatMap(site => lila.common.url.parse(site).toOption)
          .filter(_.path.sizeIs > 6)
          .fold(gameUrl)(_.toString)
        tags +
          Tag("BroadcastName", rt.tour.name.value) +
          Tag("BroadcastURL", routeUrl(rt.call)) +
          Tag("GameURL", gameUrl) +
          Tag(_.Site, site)
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
                        $doc("$gte" -> $arr("$tier", RelayTour.Tier.normal))
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
        studyRepo.publicById(rt.round.studyId).map2(rt.withStudy)
      .mapConcat(_.toList)
      .flatMapConcat(ofGames)
      .throttle(100, 1.second)

  def streamRoundGames(rs: RelayRound.WithStudy): Source[PgnStr, ?] = Source.futureSource:
    tourRepo
      .byId(rs.relay.tourId)
      .orFail(s"Missing tour for round ${rs.relay.id}")
      .map(rs.withTour)
      .map: rt =>
        val initial =
          if rt.relay.hasStarted
          then ofGames(rt).throttle(32, 1.second)
          else Source.empty[PgnStr]
        initial.concat:
          Source
            .queue[Set[StudyChapterId]](8, akka.stream.OverflowStrategy.dropHead)
            .mapMaterializedValue: queue =>
              val chan = SyncResult.busChannel(rt.relay.id)
              val sub = Bus.subscribeFunDyn(chan) { case SyncResult.Ok(chapters, _) =>
                queue.offer(chapters.view.filter(c => c.tagUpdate || c.newMoves > 0).map(_.id).toSet)
              }
              queue
                .watchCompletion()
                .addEffectAnyway:
                  Bus.unsubscribeDyn(sub, List(chan))
            .flatMapConcat(studyChapterRepo.byIdsSource)
            .throttle(16, 1.second)
            .mapAsync(1)(ofGame(rt, _))
