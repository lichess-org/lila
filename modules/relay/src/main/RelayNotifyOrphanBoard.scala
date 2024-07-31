package lila.relay

import lila.core.irc.IrcApi
import lila.study.Chapter
import com.github.blemale.scaffeine.Cache

private final class RelayNotifyOrphanBoard(api: RelayApi, irc: IrcApi)(using Executor):

  private val notifyAfterMisses = 5

  private val counter: Cache[StudyChapterId, Int] = scalalib.cache.scaffeineNoScheduler
    .expireAfterWrite(3 minutes)
    .build[StudyChapterId, Int]()

  private val once = scalalib.cache.OnceEvery[StudyChapterId](1 hour)

  def inspectPlan(rt: RelayRound.WithTour, plan: RelayUpdatePlan.Plan): Funit =
    (rt.tour.official && plan.input.games.nonEmpty).so:
      counter.invalidateAll(plan.update.map(_._1.id))
      plan.orphans.sequentiallyVoid: chapter =>
        val count = ~counter.getIfPresent(chapter.id) + 1
        if count >= notifyAfterMisses && once(chapter.id) then
          irc.broadcastOrphanBoard(rt.round.id, rt.fullName, chapter.id, chapter.name)
        else fuccess(counter.put(chapter.id, count))
