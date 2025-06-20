package lila.relay

import lila.core.notify.{ NotifyApi, NotificationContent }
import scala.util.{ Success, Failure }

final private class RelayNotifier(
    notifyApi: NotifyApi,
    tourRepo: RelayTourRepo,
    getSubscribers: lila.core.fide.GetSubscribers
)(using
    Executor
):

  private def notifyPlayerSubscribers(rt: RelayRound.WithTour, game: RelayGame): Funit =
    tourRepo
      .hasNotified(rt)
      .not
      .map: _ =>
        tourRepo
          .setNotified(rt)
          .map: _ =>
            game.fideIdsList.foreach(fid =>
              getSubscribers(fid)
                .foreach: subscribers =>
                  subscribers.nonEmpty.so:
                    notifyApi.notifyMany(
                      subscribers,
                      NotificationContent.BroadcastGame(
                        "some url to the game",
                        "Game started",
                        "The game of a player that you are following has begun"
                      )
                    )
            )

  private def notifyTournamentSubscribers(rt: RelayRound.WithTour): Funit =
    tourRepo
      .hasNotified(rt)
      .not
      .flatMapz:
        tourRepo.setNotified(rt) >>
          tourRepo
            .subscribers(rt.tour.id)
            .flatMap: subscribers =>
              subscribers.nonEmpty.so:
                notifyApi.notifyMany(
                  subscribers,
                  NotificationContent.BroadcastRound(
                    rt.path,
                    rt.tour.name.value,
                    s"${rt.round.name} has begun"
                  )
                )

  def chapterUpdated(rt: RelayRound.WithTour, game: RelayGame): Funit =
    notifyTournamentSubscribers(rt)
    notifyPlayerSubscribers(rt, game)
