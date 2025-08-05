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

      def followersOf(color: Color): Fu[Set[UserId]] =
        chapter.tags.fideIds(color).so(getPlayerFollowers)

      def notify(followers: Set[UserId], color: Color): Funit =
        val names = chapter.tags.names
        names(color) match
          case Some(playerName) =>
            val opponent = names(!color).map(name => s" against ${name} ").getOrElse(" ")
            notifyApi.notifyMany(
              followers,
              NotificationContent.BroadcastRound(
                url = rt.path(chapter.id),
                title = rt.tour.name.value,
                text = s"${playerName} is playing${opponent}in ${rt.round.name}"
              )
            )
          case None =>
            fuccess(lila.log("relay").warn(s"Missing $color player name in ${rt.path(chapter.id)}"))

      dedupNotif(chapter.id).so:
        for
          whiteFollowers <- followersOf(Color.white)
          _ <- notify(whiteFollowers, Color.white)
          blackFollowers <- followersOf(Color.black)
          newFollowers = blackFollowers -- whiteFollowers
          _ <- notify(newFollowers, Color.black)
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

  def onCreate(rt: RelayRound.WithTour, chapter: Chapter): Funit =
    notifyTournamentSubscribers(rt)
      .zip:
        (rt.tour.isPublic && rt.tour.official).so(notifyPlayerFollowers(rt, chapter))
      .void

  def onUpdate = onCreate
