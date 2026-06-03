package lila.round

import alleycats.Zero

opaque type ScheduleExpiration = Game => Unit
object ScheduleExpiration extends FunctionWrapper[ScheduleExpiration, Game => Unit]

opaque type UrgentGames = List[Pov]
object UrgentGames extends TotalWrapper[UrgentGames, List[Pov]]:
  given Zero[UrgentGames] = Zero(UrgentGames(Nil))
