package lila.bot

import lila.memo.RateLimit
import lila.memo.RateLimit.Limited
import lila.core.LightUser
import lila.core.user.LightUserApi

case class OpponentLimit(msg: String, until: Instant)
type EitherBotLimit = Limited | OpponentLimit

final class BotLimit(lightUserApi: LightUserApi)(using Executor, lila.core.config.RateLimit):

  private val max = Max(100)

  // number of bot-vs-bot games per day
  private val botGamesPerDay = RateLimit[UserId](max.value, 1.day, BotLimit.limitKey)
  import botGamesPerDay.isLimited

  lila.common.Bus.sub[lila.core.game.StartGame]: g =>
    g.users.sequence.foreach: users =>
      if users.forall(_.isBot)
      then users.foreach(u => botGamesPerDay.test(u.id))

  def challengeLimitError(orig: LightUser, dest: LightUser): Option[EitherBotLimit] =
    (orig.isBot && dest.isBot).so:
      isLimited(orig.id)
        .map: until =>
          Limited(
            BotLimit.limitKey,
            s"You played $max games against other bots today, please wait before challenging another bot.",
            until
          )
        .orElse:
          isLimited(dest.id).map: until =>
            val msg =
              s"${dest.name} played $max games against other bots today, please wait until ${showDate(until)} to challenge them."
            OpponentLimit(msg, until)

  def acceptLimitError(opponent: UserId)(using me: Me): Option[EitherBotLimit] =
    me.isBot.so:
      challengeLimitError(me.light, lightUserApi.syncFallback(opponent))

  private def showDate(i: Instant) = isoDateTimeFormatter.print(i)

object BotLimit:

  private val limitKey = "bot.vsBot.day"

  import play.api.libs.json.*
  given Writes[OpponentLimit] = Writes: l =>
    Json.obj(
      "error" -> l.msg,
      "ratelimit" -> Json.obj(
        "key" -> limitKey,
        "seconds" -> (l.until.toSeconds - nowSeconds).toInt.atLeast(0)
      )
    )
