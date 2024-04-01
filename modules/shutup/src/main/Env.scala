package lila.shutup

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.*
import lila.user.UserRepo

@Module
final class Env(
    appConfig: Configuration,
    relationApi: lila.core.relation.RelationApi,
    reportApi: lila.core.report.ReportApi,
    gameRepo: lila.core.game.GameRepo,
    userRepo: UserRepo,
    db: lila.db.Db
)(using Executor):

  private lazy val coll = db(CollName("shutup"))

  lazy val api = wire[ShutupApi]
