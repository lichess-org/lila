package lila.relay

import lila.core.notify.*

final private class RelayNotifier(notifyApi: NotifyApi, tourRepo: RelayTourRepo)(using Executor):

  def roundBegin(rt: RelayRound.WithTour): Funit =
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
                  BroadcastRound(
                    rt.path,
                    rt.tour.name.value,
                    s"${rt.round.name} has begun"
                  )
                )
