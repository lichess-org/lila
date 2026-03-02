package lila.fishnet

import lila.common.Bus

/* async wait for analysis to complete */
final class FishnetAwaiter(using Executor, Scheduler) extends lila.core.fishnet.AnalysisAwaiter:

  // returns the number of games missing when the wait is over
  def apply(gameIds: Seq[GameId], atMost: FiniteDuration): Fu[Int] =
    gameIds.nonEmpty.so:
      val promise = Promise[Int]()
      var remainingIds = gameIds.toSet
      val listener = Bus.sub[lila.analyse.actorApi.AnalysisReady]:
        case lila.analyse.actorApi.AnalysisReady(game, _) if remainingIds(game.id) =>
          remainingIds = remainingIds - game.id
          if remainingIds.isEmpty then promise.success(0)
      promise.future
        .withTimeoutDefault(atMost, remainingIds.size)
        .addEffectAnyway:
          Bus.unsub[lila.analyse.actorApi.AnalysisReady](listener)
