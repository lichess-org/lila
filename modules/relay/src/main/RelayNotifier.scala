package lila.relay

final private class RelayNotifier(notifyApi: lila.notify.NotifyApi, tourRepo: RelayTourRepo)(using Executor):

  def roundBegin(rt: RelayRound.WithTour): Funit =
    tourRepo.hasNotified(rt) collect:
      case false =>
        tourRepo.setNotified(rt) >>
          tourRepo
            .subscribers(rt.tour.id)
            .flatMap: subscribers =>
              notifyApi.notifyMany(
                subscribers,
                lila.notify.BroadcastRound(
                  s"/broadcast/${rt.tour.slug}/${rt.round.slug}/${rt.round.id}",
                  s"${rt.tour.name} round ${rt.round.name} has begun",
                  none
                )
              )
