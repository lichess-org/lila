package lila.oidc

import lila.identity.{ IdentityStatus, IdentityStatusRecord, IdentityStatusRepo }
import lila.user.{ User, UserRepo }
import lila.core.userId.*

/** Single provisioning path: external identity -> lila user.
  * OIDC and the future signature flow must use it.
  */
final class UserProvisioning(
    userRepo: UserRepo,
    statusRepo: IdentityStatusRepo
)(using Executor):

  def resolve(identity: ExternalIdentity): Fu[LoginOutcome] =
    if identity.subject.isEmpty || identity.username.isEmpty then
      fuccess(LoginOutcome.Refused(LoginError.ClaimsError, None))
    else
      val userId = UserId(identity.subject)
      statusRepo.get(userId).flatMap { stored =>
        stored.flatMap(statusError) match
          case Some(error) => fuccess(LoginOutcome.Refused(error, None))
          case None =>
            userRepo.byId(userId).flatMap {
              case Some(user) => resolveExisting(user, identity, stored)
              case None       => create(identity)
            }
      }

  private def statusError(record: IdentityStatusRecord): Option[LoginError] =
    record.status match
      case IdentityStatus.Deleted                           => Some(LoginError.Deleted)
      case IdentityStatus.Banned | IdentityStatus.Suspended => Some(LoginError.Banned)
      case IdentityStatus.Active                            => None

  private def resolveExisting(
      user: User,
      identity: ExternalIdentity,
      stored: Option[IdentityStatusRecord]
  ): Fu[LoginOutcome] =
    val globallyEnabled =
      if identity.globallyEnabled.contains(false) || stored.exists(!_.globallyEnabled) then Some(false)
      else identity.globallyEnabled.orElse(stored.map(_.globallyEnabled))
    for
      deleted       <- userRepo.isDeleted(user)
      foreverClosed <- userRepo.isForeverClosed(user)
      result <-
        LoginError.fromStatus(user.enabled.yes, foreverClosed, deleted, globallyEnabled) match
          case Some(error) => fuccess(LoginOutcome.Refused(error, Some(user)))
          case None        => sync(user, identity)
    yield result

  private def sync(user: User, identity: ExternalIdentity): Fu[LoginOutcome] =
    val updateUsername =
      if identity.username != user.username.value then userRepo.setUsername(user.id, identity.username)
      else funit
    val updateEmail =
      if !identity.emailVerified then funit
      else
        identity.email match
          case None => funit
          case Some(email) =>
            userRepo.email(user.id).flatMap: current =>
              if current.exists(_.normalize == email.normalize) then userRepo.setEmailConfirmed(user.id).void
              else userRepo.setEmail(user.id, email) >> userRepo.setEmailConfirmed(user.id).void
    for
      _         <- updateUsername
      _         <- updateEmail
      refreshed <- userRepo.byId(user.id)
      result <- refreshed match
        case Some(u) if u.enabled.yes => fuccess(LoginOutcome.Success(u))
        case Some(u)                  => fuccess(LoginOutcome.Refused(LoginError.Closed, Some(u)))
        case None                     => fuccess(LoginOutcome.Refused(LoginError.Deleted, None))
    yield result

  private def create(identity: ExternalIdentity): Fu[LoginOutcome] =
    userRepo
      .createExternal(UserId(identity.subject), identity.username, identity.email)
      .flatMap {
        case Some(user) => fuccess(LoginOutcome.Success(user))
        case None =>
          userRepo.byId(UserId(identity.subject)).map:
            _.fold[LoginOutcome](LoginOutcome.Refused(LoginError.ProviderError, None)): user =>
              LoginOutcome.Success(user)
      }
