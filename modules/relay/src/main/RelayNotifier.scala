package lila.relay

final private class RelayNotifier(notifyApi: lila.notify.NotifyApi, tourRepo: RelayTourRepo)(using Executor):

  def roundBegin(rt: RelayRound.WithTour): Funit =
    !tourRepo.hasNotified(rt) flatMapz:
      tourRepo.setNotified(rt) >>
        tourRepo
          .subscribers(rt.tour.id)
          .flatMap: subscribers =>
            notifyApi.notifyMany(
              subscribers,
              lila.notify.BroadcastRound(
                rt.path,
                rt.tour.name,
                s"${rt.round.name} has begun"
              )
            )
