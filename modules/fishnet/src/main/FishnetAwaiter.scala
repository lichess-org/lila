package lila.fishnet

import lila.common.Bus

/* async wait for analysis to complete */
final class FishnetAwaiter(using Executor, Scheduler) extends lila.core.fishnet.AnalysisAwaiter:

  private val busChannel = "analysisReady"

  def apply(gameIds: Seq[GameId], atMost: FiniteDuration): Funit =
    gameIds.nonEmpty.so:
      val promise      = Promise[Unit]()
      var remainingIds = gameIds.toSet
      val listener = Bus.subscribeFun(busChannel):
        case lila.analyse.actorApi.AnalysisReady(game, _) if remainingIds(game.id) =>
          remainingIds = remainingIds - game.id
          if remainingIds.isEmpty then promise.success {}
      promise.future
        .withTimeoutDefault(atMost, {})
        .addEffectAnyway:
          Bus.unsubscribe(listener, busChannel)
