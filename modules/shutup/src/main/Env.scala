package lila.shutup

import com.softwaremill.macwire.*
import lila.core.config.*

@Module
final class Env(
    relationApi: lila.core.relation.RelationApi,
    reportApi: lila.core.report.ReportApi,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    db: lila.db.Db
)(using Executor):

  private val coll = db(CollName("shutup"))

  val analyser = Analyser

  val api = wire[ShutupApi]
