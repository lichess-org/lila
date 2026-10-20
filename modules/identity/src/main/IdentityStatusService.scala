package lila.identity

import lila.core.security.{ CloseAccount, RevokeAllCredentials }

final class IdentityStatusService(
    statusRepo: IdentityStatusRepo,
    eventRepo: IdentityEventRepo,
    userRepo: lila.user.UserRepo
)(using Executor):

  /** Apply a signed IdP event exactly once and in version order. Returns true when the projection changed.
    */
  def apply(event: IdentityEvent): Fu[Boolean] =
    eventRepo.register(event).flatMap {
      case false => fuccess(false)
      case true =>
        statusRepo.applyEvent(event).flatMap { changed =>
          if !changed then fuccess(false)
          else
            if !event.status.globallyEnabled then
              lila.common.Bus.publish(RevokeAllCredentials(event.subject), "identityStatus")
              lila.common.Bus.publish(CloseAccount(event.subject), "accountClose")
            logger.info(
              s"identity event ${event.eventId.take(8)} sub=${event.subject} status=${event.status.key} v=${event.version}"
            )
            val cleanup =
              if event.status == IdentityStatus.Deleted then userRepo.anonymize(event.subject)
              else if event.status == IdentityStatus.Active then restoreDeleted(event.subject)
              else funit
            cleanup.map(_ => true)
        }
    }

  /** A deleted identity may come back as active: re-enable only local tombstones, never locally closed or
    * mod-closed accounts.
    */
  private def restoreDeleted(userId: UserId): Funit =
    userRepo
      .byId(userId)
      .flatMap:
        case Some(user) => userRepo.isDeleted(user).flatMap(if _ then userRepo.reopen(user.id) else funit)
        case None       => funit
