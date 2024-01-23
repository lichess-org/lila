package lila.relay

import chess.format.pgn.{ Tag, Tags }
import chess.format.UciPath
import lila.common.licon
import lila.notify.{ BroadcastRound }
import lila.socket.Socket.Sri
import lila.study.*
import lila.tree.Branch

final private class RelayNotifier(
    notifyApi: lila.notify.NotifyApi,
    tourRepo: RelayTourRepo
)(using Executor):

  def roundBegin(rt: RelayRound.WithTour): Funit =
    tourRepo
      .subscribers(rt.tour.id)
      .flatMap { subscribers =>
        notifyApi.notifyMany(
          subscribers,
          BroadcastRound(
            s"/broadcast/${rt.tour.slug}/${rt.round.slug}/${rt.round.id}",
            s"${rt.tour.name} round ${rt.round.name} has begun",
            none
          )
        )
      }
      .void
