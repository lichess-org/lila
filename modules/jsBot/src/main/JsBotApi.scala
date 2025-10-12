package lila.jsBot

import lila.memo.CacheApi
import lila.memo.CacheApi.buildAsyncTimeout
import lila.core.perm.Granter

final private class JsBotApi(repo: JsBotRepo, cacheApi: CacheApi)(using Executor, Scheduler):

  object playable:

    object keys:
      val keysForDev = BotKey.from:
        List("centipawn", "tal-e", "terrence", "howard", "professor", "lila")
      val keysForBeta = BotKey.from:
        List("centipawn", "tal-e")
      def forMe(isInBetaTeam: Me => Fu[Boolean])(using me: Option[Me]): Fu[List[BotKey]] =
        if Granter.opt(_.BotEditor) then fuccess(keysForDev)
        else if Granter.opt(_.Beta) then fuccess(keysForBeta)
        else me.so(isInBetaTeam).mapz(keysForBeta)

    private val all = cacheApi.unit[List[BotJson]]:
      _.refreshAfterWrite(1.minute).buildAsyncTimeout(): _ =>
        repo.getLatestBots()

    def get(isInBetaTeam: Me => Fu[Boolean])(using Option[Me]): Fu[List[BotJson]] = for
      myKeys <- keys.forMe(isInBetaTeam)
      allBots <- all.get({})
      myBots = allBots.filter(b => myKeys.contains(b.key)).sortLike(myKeys, _.key)
    yield myBots
