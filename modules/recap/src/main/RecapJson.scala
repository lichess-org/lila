package lila.recap

import play.api.libs.json.*
import lila.common.Json.{ *, given }
import chess.ByColor
import lila.common.SimpleOpening
import lila.common.LilaOpeningFamily
import lila.core.game.Source
import lila.core.user.LightUserApi

private final class RecapJson(lightUserApi: LightUserApi)(using Executor):

  def apply(recap: Recap, user: User) =
    for _ <- lightUserApi.preloadMany(recap.userIds)
    yield Json.obj("recap" -> recap, "user" -> user.light)

  def apply(user: User) = Json.obj("user" -> user.light)

  given Writes[UserId] = writeAs(lightUserApi.syncFallback)

  given [A: Writes]: Writes[Recap.Counted[A]] = Json.writes

  given Writes[LilaOpeningFamily] = new:
    def writes(o: LilaOpeningFamily): JsObject =
      Json.obj("key" -> o.key, "name" -> o.name)

  given Writes[FiniteDuration] = writeAs(_.toSeconds)

  given Writes[Map[Source, Int]] = writeAs(_.mapKeys(_.name))

  given Writes[SimpleOpening] = new:
    def writes(o: SimpleOpening): JsObject =
      Json.obj(
        "key"  -> o.key,
        "name" -> o.name,
        "pgn"  -> o.ref.pgn
      )

  given Writes[NbWin]        = Json.writes
  given Writes[PuzzleVotes]  = Json.writes
  given Writes[RecapPuzzles] = Json.writes
  given Writes[Recap.Perf]   = Json.writes
  given Writes[RecapGames]   = Json.writes
  given Writes[Recap]        = Json.writes
