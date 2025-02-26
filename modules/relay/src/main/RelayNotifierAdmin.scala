package lila.relay

import com.github.blemale.scaffeine.Cache

import lila.study.ChapterPreviewApi
import lila.core.irc.IrcApi

private final class RelayNotifierAdmin(api: RelayApi, irc: IrcApi, previewApi: ChapterPreviewApi)(using
    ex: Executor,
    scheduler: Scheduler
):

  object orphanBoards:

    private val notifyAfterMisses = 10

    private val counter: Cache[StudyChapterId, Int] = scalalib.cache.scaffeineNoScheduler
      .expireAfterWrite(3.minutes)
      .build[StudyChapterId, Int]()

    private val once = scalalib.cache.OnceEvery[StudyChapterId](1.hour)

    def inspectPlan(rt: RelayRound.WithTour, plan: RelayUpdatePlan.Plan): Funit =
      (rt.tour.official && plan.input.games.nonEmpty && !rt.round.sync.upstream.exists(_.isInternal)).so:
        counter.invalidateAll(plan.update.map(_._1.id))
        plan.orphans.sequentiallyVoid: chapter =>
          val count = ~counter.getIfPresent(chapter.id) + 1
          if count >= notifyAfterMisses && once(chapter.id) then
            irc.broadcastOrphanBoard(rt.round.id, rt.fullName, chapter.id, chapter.name)
          else fuccess(counter.put(chapter.id, count))

  object tooManyGames:

    private val once = scalalib.cache.OnceEvery[RelayRoundId](1.hour)

    def apply(rt: RelayRound.WithTour, games: Int, max: Max): Funit =
      once(rt.round.id).so:
        irc.broadcastError(rt.round.id, rt.fullName, s"Too many games from source: $games. Max is $max")

  object missingFideIds:
    private val once = scalalib.cache.OnceEvery[RelayRoundId](1.hour)

    def schedule(id: RelayRoundId) =
      if once(id) then
        scheduler.scheduleOnce(1.minute):
          api.byIdWithTour(id).flatMapz(checkNow)

    private def checkNow(rt: RelayRound.WithTour): Funit =
      if rt.round.sync.upstream.exists(_.isInternal)
      then funit
      else
        previewApi
          .dataList(rt.round.studyId)
          .flatMap: chapters =>
            val missing: List[(StudyChapterId, String)] = chapters.flatMap: chapter =>
              chapter.players
                .so(_.toList)
                .filter(_.fideId.isEmpty)
                .map: player =>
                  (chapter.id, player.name.fold("?")(_.value))
            missing.nonEmpty.so:
              irc.broadcastMissingFideId(rt.round.id, rt.fullName, missing)
