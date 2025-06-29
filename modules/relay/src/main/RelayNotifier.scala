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

    def apply(rt: RelayRound.WithTour, chapter: Chapter, game: RelayGame): Unit =
      dedupNotif(chapter.id).so:
        game.fideIds.foreach: (color, fid) =>
          fid.so: fid =>
            for
              followers <- getPlayerFollowers(fid)
              _         <- followers.nonEmpty.so(notify(followers, color, fid))
            yield ()

      def notify(followers: List[UserId], color: Color, fid: chess.FideId) =
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
            fuccess(
              lila.log("relay").warn(s"Missing player name for FIDE id ${fid} in game ${chapter.id}")
            )

  private object notifyTournamentSubscribers:

    private val dedupDbReq = OnceEvery[RelayRoundId](5.minutes)

    def apply(rt: RelayRound.WithTour): Unit =
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
