package lila.analyse

import com.softwaremill.macwire.*

import lila.common.config.*

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.game.GameRepo,
    cacheApi: lila.memo.CacheApi,
    net: NetConfig
)(using Executor):

  lazy val analysisRepo = AnalysisRepo(db(CollName("analysis2")))

  lazy val requesterApi = RequesterApi(db(CollName("analysis_requester")))

  lazy val analyser = wire[Analyser]

  lazy val annotator = Annotator(net.domain)

  lazy val externalEngine = ExternalEngineApi(db(CollName("external_engine")), cacheApi)

  lila.common.Bus.subscribeFun("oauth") { case lila.hub.actorApi.oauth.TokenRevoke(id) =>
    externalEngine onTokenRevoke id
  }
