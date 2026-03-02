package lila.jsBot

import lila.memo.CacheApi
import lila.memo.CacheApi.{ buildAsyncTimeout, invalidateAll }
import lila.core.perm.Granter

final private class JsBotApi(repo: JsBotRepo, cacheApi: CacheApi)(using Executor, Scheduler):

  def put(bot: BotJson)(using me: Me): Fu[BotJson] =
    repo
      .putBot(bot, me.userId)
      .andDo:
        playable.all.invalidateAll()

  object playable:

    private def forMe(isInBetaTeam: MyId ?=> Fu[Boolean])(using me: Option[Me]): Fu[List[BotKey]] =
      if Granter.opt(_.BotEditor) then fuccess(publicBotKeys ::: betaBotKeys ::: devBotKeys)
      else if Granter.opt(_.Beta) then fuccess(publicBotKeys ::: betaBotKeys)
      else
        for beta <- me.soUse(isInBetaTeam)
        yield publicBotKeys ::: beta.so(betaBotKeys)

    private[JsBotApi] val all = cacheApi.unit[List[BotJson]]:
      _.refreshAfterWrite(1.minute).buildAsyncTimeout(): _ =>
        repo.getLatestBots()

    def get(isInBetaTeam: MyId ?=> Fu[Boolean])(using Option[Me]): Fu[List[BotJson]] = for
      myKeys <- forMe(isInBetaTeam)
      allBots <- all.get({})
      myBots = allBots.filter(b => myKeys.contains(b.key)).sortLike(myKeys, _.key)
    yield myBots
