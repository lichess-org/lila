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

    def apply(rt: RelayRound.WithTour, chapter: Chapter, game: RelayGame): Funit =
      dedupNotif(chapter.id).so:
        val futureByColor = game.fideIds.mapWithColor: (color, fid) =>
          for
            followers <- fid.so(getPlayerFollowers)
            notify    <- followers.nonEmpty.so:
              notifyApi.notifyMany(
                followers,
                NotificationContent.BroadcastRound(
                  url = rt.path(chapter.id),
                  title = rt.tour.name.value,
                  text = chapter.tags.names
                    .traverse(identity)
                    .match
                      case Some(players) =>
                        s"${players(color)} is playing against ${players(!color)} in ${rt.round.name}"
                      case None => s"A player you are following is playing in ${rt.round.name}"
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

  def chapterUpdated(rt: RelayRound.WithTour, chapter: Chapter, game: RelayGame): Unit =
    notifyPlayerFollowers(rt, chapter, game)
    notifyTournamentSubscribers(rt)
