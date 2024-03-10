package lila.relay

import lila.study.ChapterRepo
import lila.fide.FidePlayer
import lila.db.dsl.{ *, given }
import lila.common.paginator.Paginator

final class RelayPlayerTour(
    colls: RelayColls,
    chapterRepo: ChapterRepo,
    pager: RelayPager,
    cacheApi: lila.memo.CacheApi,
    playerApi: lila.fide.FidePlayerApi // for initial guessAndDenormalizeFideIds
)(using Executor, akka.stream.Materializer):

  private val tourIdsCache = cacheApi[chess.FideId, List[RelayTour.Id]](1024, "relay.player.tourIds"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture: fideId =>
      chapterRepo
        .studyIdsByRelayFideId(fideId)
        .flatMap: studyIds =>
          colls.round.distinctEasy[RelayTour.Id, List]("tourId", $inIds(studyIds))

  def playerTours(player: FidePlayer, page: Int): Fu[Paginator[RelayTour.WithLastRound]] =
    tourIdsCache
      .get(player.id)
      .flatMap:
        pager.byIds(_, page)

  guessAndDenormalizeFideIds() // needs only run once. Can be removed then.

  private def guessAndDenormalizeFideIds(): Funit =
    import reactivemongo.akkastream.cursorProducer
    import akka.stream.scaladsl.*
    import lila.study.Chapter
    import lila.study.BSONHandlers.given
    import lila.common.LilaStream
    chapterRepo.coll: chapterColl =>
      colls.tour
        .find($doc("tier" -> $exists(true)), $doc("_id" -> true).some)
        .cursor[Bdoc]()
        .documentSource()
        .mapConcat:
          _.getAsOpt[RelayTour.Id]("_id").toList
        .mapAsync(1): tourId =>
          colls.round.distinctEasy[RelayRoundId, List]("_id", $doc("tourId" -> tourId))
        .mapAsync(1): roundIds =>
          roundIds.nonEmpty.so:
            chapterColl
              .find(
                $doc(
                  "studyId".$in(roundIds),
                  "tags".$regex("^(White|Black):"),
                  "relay.fideIds".$exists(false),
                  "relay.lastMoveAt".$exists(true) // rare; and breaks the bson reader
                ),
                $doc("relay" -> true, "tags" -> true).some
              )
              .cursor[Chapter.RelayAndTags]()
              .listAll()
        .mapConcat(identity)
        .mapAsync(4): chap =>
          chap.tags.names
            .zip(chap.tags.titles)
            .traverse: (name, title) =>
              playerApi.guessPlayer(none, name, title)
            .map:
              _.mapList(_.so(_.id.value))
            .flatMap: fideIds =>
              fideIds
                .exists(_ > 0)
                .so:
                  chapterColl.update
                    .one(
                      $id(chap.id),
                      $set("relay.fideIds" -> fideIds, "relay.fideIdsGuess" -> true)
                    )
                    .void
        .runWith(LilaStream.sinkCount)
        .map: nb =>
          println(s"Denormalized Fide IDs of $nb chapters")
