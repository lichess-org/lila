package lila.jsBot

import lila.memo.CacheApi
import lila.memo.CacheApi.buildAsyncTimeout

final private class JsBotApi(repo: JsBotRepo, cacheApi: CacheApi)(using Executor, Scheduler):

  object playable:

    private val cache = cacheApi.unit[List[BotJson]]:
      _.refreshAfterWrite(1.minute).buildAsyncTimeout(): _ =>
        repo.getLatestBots()

    def get: Fu[List[BotJson]] = cache.get({})
