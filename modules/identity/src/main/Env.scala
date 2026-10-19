package lila.identity

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.core.config.{ CollName, Secret }

@Module
final class Env(db: lila.db.Db, appConfig: Configuration)(using Executor):

  val statusRepo = IdentityStatusRepo(db(CollName("identity_status")))
  val eventRepo  = IdentityEventRepo(db(CollName("identity_event")))

  /** Empty secret disables the internal events endpoint. */
  val webhookSecret: Option[Secret] =
    appConfig
      .getOptional[String]("identity.webhook.secret")
      .map(Secret.apply)
      .filter(_.value.nonEmpty)

  lazy val statusService = IdentityStatusService(statusRepo, eventRepo)
