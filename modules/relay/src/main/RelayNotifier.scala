package lila.relay

import scalalib.cache.OnceEvery

import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.study.Chapter

final private class RelayNotifier(
    notifyApi: NotifyApi,
    tourRepo: RelayTourRepo,
    getPlayerFollowers: lila.core.fide.GetPlayerFollowers
)(using Executor):

  private object notifyPlayerFollowers:

    private val dedupNotif = OnceEvery[StudyChapterId](1.day)

    def apply(rt: RelayRound.WithTour, chapter: Chapter): Funit =
      dedupNotif(chapter.id).so:
        val futureByColor = chapter.tags.fideIds.mapWithColor: (color, fid) =>
          for
            followers <- fid.so(getPlayerFollowers)
            notify    <- followers.nonEmpty.so:
              chapter.tags.names.sequence.so: names =>
                notifyApi.notifyMany(
                  followers,
                  NotificationContent.BroadcastRound(
                    url = rt.path(chapter.id),
                    title = rt.tour.name.value,
                    text = s"${names(color)} is playing against ${names(!color)} in ${rt.round.name}"
                  )
                )
          yield notify
        Future.sequence(futureByColor.all).void

  private object notifyTournamentSubscribers:

    private val dedupDbReq = OnceEvery[RelayRoundId](5.minutes)

    def apply(rt: RelayRound.WithTour): Funit =
      dedupDbReq(rt.round.id).so:
        tourRepo
          .hasNotified(rt)
          .not
          .flatMapz:
            for
              _           <- tourRepo.setNotified(rt)
              subscribers <- tourRepo.subscribers(rt.tour.id)
              _           <- subscribers.nonEmpty.so:
                notifyApi.notifyMany(
                  subscribers,
                  NotificationContent.BroadcastRound(
                    rt.path,
                    rt.tour.name.value,
                    s"${rt.round.name} has begun"
                  )
                )
            yield ()

  def onCreate(rt: RelayRound.WithTour, chapter: Chapter): Unit =
    notifyTournamentSubscribers(rt)
    if rt.tour.tier.exists(_ >= RelayTour.Tier.normal)
    then notifyPlayerFollowers(rt, chapter)

  def onUpdate = onCreate
