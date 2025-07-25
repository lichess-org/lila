package lila.bot

import lila.ui.Context
import lila.common.HTTPRequest
import lila.memo.SettingStore
import scala.util.matching.Regex
import scalalib.ThreadLocalRandom
import chess.IntRating

private final class BoardReport(settingStore: SettingStore.Builder)(using
    ec: Executor,
    scheduler: Scheduler
):

  import SettingStore.Regex.given
  val domainSetting = settingStore[Regex](
    "boardApiBadRefererRegex",
    default = "-".r,
    text = "Board API: referer domains that use engine, as a regex".some
  )

  def move(game: Game)(using ctx: Context) = for
    me <- ctx.me
    rating <- game.player(me).flatMap(_.rating)
    if checkNow(game)
    ref <- HTTPRequest.referer(ctx.req)
    url <- lila.common.url.parse(ref).toOption
    domain = url.host.toString
    if domainSetting.get().matches(domain)
  yield found(me, game, rating, ref)

  private def found(me: Me, game: Game, rating: IntRating, ref: String): Unit =
    val delayBase =
      if rating > IntRating(2500) then 0
      else if rating > IntRating(2300) then 1
      else if rating > IntRating(2000) then 6
      else if rating > IntRating(1800) then 12
      else 24
    val minutes = (2 + delayBase + ThreadLocalRandom.nextInt(delayBase * 60))
    lila
      .log("cheat")
      .branch("board")
      .warn:
        s"Marking https://lichess.org/@/${me.username} for https://lichess.org/${game.id} with $ref in $minutes minutes"
    scheduler.scheduleOnce(minutes.minutes):
      lila.common.Bus.pub(lila.core.mod.BoardApiMark(me.userId, ref))

  private def checkNow(game: Game): Boolean =
    game.ply.value == 5 + (game.createdAt.toSeconds % 20)
