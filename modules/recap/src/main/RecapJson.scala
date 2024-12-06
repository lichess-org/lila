package lila.recap

import play.api.libs.json.*
import lila.common.Json.{ *, given }
import chess.ByColor
import lila.common.SimpleOpening
import lila.common.LilaOpeningFamily
import lila.core.game.Source

object RecapJson:

  given [A: Writes]: Writes[ByColor[A]] = new:
    def writes(o: ByColor[A]): JsObject =
      Json.obj("white" -> o.white, "black" -> o.black)

  given [A: Writes]: Writes[Recap.Counted[A]] = Json.writes

  given Writes[LilaOpeningFamily] = new:
    def writes(o: LilaOpeningFamily): JsObject =
      Json.obj("key" -> o.key, "name" -> o.name)

  given Writes[FiniteDuration] = writeAs(_.toSeconds)

  given Writes[Map[Source, Int]] = writeAs(_.mapKeys(_.name))

  given Writes[SimpleOpening] = Json.writes
  given Writes[NbAndStreak]   = Json.writes
  given Writes[Results]       = Json.writes
  given Writes[PuzzleVotes]   = Json.writes
  given Writes[RecapPuzzles]  = Json.writes
  given Writes[Recap.Perf]    = Json.writes
  given Writes[RecapGames]    = Json.writes
  given Writes[Recap]         = Json.writes
