package lila.relay

import lila.core.notify.{ NotifyApi, NotificationContent }
import lila.study.{ Chapter, ChapterRepo }
import chess.ByColor

final private class RelayNotifier(
    notifyApi: NotifyApi,
    tourRepo: RelayTourRepo,
    chapterRepo: ChapterRepo,
    getPlayerFollowers: lila.core.fide.GetPlayerFollowers
)(using Executor):

  private def notifyPlayerFollowers(rt: RelayRound.WithTour, chapter: Chapter, game: RelayGame): Funit =
    chapterRepo
      .hasNotified(chapter.id)
      .not
      .flatMapz:
        chapterRepo.setNotified(chapter.id) >>
        val futureByColor = game.fideIds.mapWithColor((color, fid) =>
          fid
            .map(
              getPlayerFollowers(_).flatMap(followers =>
                if followers.nonEmpty then
                  notifyApi.notifyMany(
                    followers,
                    NotificationContent.BroadcastRound(
                      rt.path(chapter.id),
                      rt.tour.name.value,
                      chapter.players.flatMap(players =>
                        players.map(_.name) match
                          case ByColor(Some(whiteName), Some(blackName)) =>
                            Some(ByColor(whiteName, blackName))
                          case _ => None
                      ) match
                        case Some(players) =>
                          s"${players(color)} is playing against ${players(!color)} in ${rt.round.name}"
                        case None => s"A player you are following has started a game in ${rt.round.name}"
                    )
                  )
                else Future.successful(())
              )
            )
            .getOrElse(Future.successful(()))
        )
        Future.sequence(futureByColor.all).map(_ => ())

  private object notifyTournamentSubscribers:

    private val dedupDbReq = scalalib.cache.OnceEvery[RelayTourId](5.minutes)

    def apply(rt: RelayRound.WithTour): Funit =
      dedupDbReq(rt.tour.id).so:
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
