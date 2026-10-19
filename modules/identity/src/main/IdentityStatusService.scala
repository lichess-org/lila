package lila.identity

import lila.core.security.{ CloseAccount, RevokeAllCredentials }

final class IdentityStatusService(
    statusRepo: IdentityStatusRepo,
    eventRepo: IdentityEventRepo
)(using Executor):

  /** Apply a signed IdP event exactly once and in version order.
    * Returns true when the projection changed.
    */
  def apply(event: IdentityEvent): Fu[Boolean] =
    eventRepo.register(event).flatMap {
      case false => fuccess(false)
      case true =>
        statusRepo.applyEvent(event).map { changed =>
          if changed then
            logger.info(
              s"identity event ${event.eventId.take(8)} sub=${event.subject} status=${event.status.key} v=${event.version}"
            )
            if !event.status.globallyEnabled then
              lila.common.Bus.publish(RevokeAllCredentials(event.subject), "identityStatus")
              lila.common.Bus.publish(CloseAccount(event.subject), "accountClose")
          changed
        }
    }
