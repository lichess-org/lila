package lila.analyse

import com.softwaremill.macwire.*

import lila.core.config.{ CollName, NetConfig }
import lila.core.misc.analysis.MyEnginesAsJson

@Module
final class Env(
    db: lila.db.Db,
    gameRepo: lila.core.game.GameRepo,
    cacheApi: lila.memo.CacheApi,
    net: NetConfig
)(using Executor):

  lazy val analysisRepo = AnalysisRepo(db(CollName("analysis2")))

  lazy val requesterApi = RequesterApi(db(CollName("analysis_requester")))

  lazy val analyser = wire[Analyser]

  lazy val annotator = Annotator(net.domain)

  lazy val externalEngine = ExternalEngineApi(db(CollName("external_engine")), cacheApi)

  val enginesAsJson = MyEnginesAsJson(externalEngine.myExternalEnginesAsJson)

  val jsonView = JsonView

  lila.common.Bus.subscribeFun("oauth"):
    case lila.core.misc.oauth.TokenRevoke(id) => externalEngine.onTokenRevoke(id)
