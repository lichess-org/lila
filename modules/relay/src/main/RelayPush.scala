package lila.relay

import akka.actor._
import scala.concurrent.duration._

import lila.study.MultiPgn
import lila.hub.EarlyMultiThrottler

final class RelayPush(sync: RelaySync, api: RelayApi)(implicit
    system: ActorSystem,
    ec: scala.concurrent.ExecutionContext
) {

  private val throttler = system.actorOf(Props(new EarlyMultiThrottler(logger = logger)))

  def apply(rt: RelayRound.WithTour, pgn: String): Fu[Option[String]] =
    if (rt.relay.sync.hasUpstream)
      fuccess("The relay has an upstream URL, and cannot be pushed to.".some)
    else
      fuccess {
        throttler ! EarlyMultiThrottler.Work(
          id = rt.relay.id.value,
          run = () => pushNow(rt, pgn),
          cooldown = if (rt.tour.official) 3.seconds else 7.seconds
        )
        none
      }

  private def pushNow(rt: RelayRound.WithTour, pgn: String): Funit =
    RelayFetch
      .multiPgnToGames(MultiPgn.split(pgn, RelayFetch.maxChapters(rt.tour)))
      .flatMap {
        sync(rt, _)
      }
      .map { res =>
        SyncLog.event(res.moves, none)
      }
      .recover { case e: Exception =>
        SyncLog.event(0, e.some)
      }
      .flatMap { event =>
        api.update(rt.relay)(_.withSync(_ addLog event)).void
      }
}
