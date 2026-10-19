package lila.identity

import com.softwaremill.macwire.*

import lila.core.config.CollName

@Module
final class Env(db: lila.db.Db)(using Executor):

  val statusRepo = IdentityStatusRepo(db(CollName("identity_status")))
  val eventRepo  = IdentityEventRepo(db(CollName("identity_event")))
