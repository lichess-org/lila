package lila.bot

import lila.memo.RateLimit
import lila.memo.RateLimit.Limited

final class BotLimit(using Executor, lila.core.config.RateLimit):

  private val limitKey = "bot.vsBot.day"
  private val max = Max(100)

  // number of bot-vs-bot games per day
  private val botGamesPerDay = RateLimit[UserId](max.value, 1.day, limitKey)
  import botGamesPerDay.isLimited

  lila.common.Bus.sub[lila.core.game.StartGame]: g =>
    g.users.sequence.foreach: users =>
      if users.forall(_.isBot)
      then users.foreach(u => botGamesPerDay.test(u.id))

  def challengeLimitError(orig: User, dest: User): Option[Limited | String] =
    (orig.isBot && dest.isBot).so:
      isLimited(orig.id)
        .map: until =>
          Limited(
            limitKey,
            s"You played $max games against other bots today, please wait before challenging another bot.",
            until
          )
        .orElse:
          isLimited(dest.id).map: until =>
            s"${dest.username} played $max games against other bots today, please wait until ${showDate(until)} to challenge them."

  private def showDate(i: Instant) = isoDateTimeFormatter.print(i)
