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

    private val dedupNotif = OnceEvery[(StudyChapterId, Color)](1.day)

    def ofColor(rt: RelayRound.WithTour, chapter: Chapter)(color: Color): Funit =
      chapter.tags
        .fideIds(color)
        .zip(chapter.tags.names(color))
        .so: (fideId, name) =>
          dedupNotif(chapter.id -> color).so:
            for
              followers <- getPlayerFollowers(fideId)
              opponent = chapter.tags.names(!color).map(name => s" against ${name} ").getOrElse(" ")
              _ <- notifyApi.notifyMany(
                followers,
                NotificationContent.BroadcastRound(
                  url = rt.path(chapter.id),
                  title = rt.tour.name.value,
                  text = s"${name} is playing${opponent}in ${rt.round.name}"
                )
              )
            yield ()

  private object notifyTournamentSubscribers:

    private val dedupDbReq = OnceEvery[RelayRoundId](5.minutes)

    def apply(rt: RelayRound.WithTour): Funit =
      dedupDbReq(rt.round.id).so:
        tourRepo
          .hasNotified(rt)
          .not
          .flatMapz:
            for
              _ <- tourRepo.setNotified(rt)
              subscribers <- tourRepo.subscribers(rt.tour.id)
              _ <- subscribers.nonEmpty.so:
                notifyApi.notifyMany(
                  subscribers,
                  NotificationContent.BroadcastRound(
                    rt.path,
                    rt.tour.name.value,
                    s"${rt.round.name} has begun"
                  )
                )
            yield ()

  def onCreate(rt: RelayRound.WithTour, chapter: Chapter): Funit = for
    _ <- notifyTournamentSubscribers(rt)
    _ <- (rt.tour.isPublic && rt.tour.official).so:
      Color.all.traverse(notifyPlayerFollowers.ofColor(rt, chapter))
  yield ()

  def onUpdate = onCreate
