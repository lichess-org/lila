package lila.round

import alleycats.Zero

opaque type ScheduleExpiration = Game => Unit
object ScheduleExpiration extends FunctionWrapper[ScheduleExpiration, Game => Unit]

opaque type UrgentGames = List[Pov]
object UrgentGames extends TotalWrapper[UrgentGames, List[Pov]]:
  given Zero[UrgentGames] = Zero(UrgentGames(Nil))
  extension (u: UrgentGames)
    def selectNext(from: Game): Option[Pov] =
      u.value.find: pov =>
        pov.isMyTurn &&
          (pov.game.hasClock || !from.hasClock) &&
          (pov.game.nonAi || from.hasAi)
