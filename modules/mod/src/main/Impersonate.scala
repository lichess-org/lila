package lila.mod

import lila.common.Bus
import lila.core.mod.Impersonate
import lila.user.{ Me, User, UserRepo }

final class ImpersonateApi(userRepo: UserRepo):

  private var impersonations = Map.empty[ModId, UserId]

  def start(modId: ModId, user: User): Unit =
    stop(modId)
    impersonations = impersonations + (modId -> user.id)
    logger.info(s"$modId starts impersonating ${user.username}")
    Bus.pub(Impersonate(modId, user.id, true))

  def stop(modId: ModId): Unit =
    impersonations.get(modId).foreach { userId =>
      impersonations = impersonations - modId
      logger.info(s"$modId stops impersonating $userId")
      Bus.pub(Impersonate(modId, userId, false))
    }

  def impersonating(modId: ModId): Fu[Option[User]] = impersonations.get(modId).so(userRepo.byId)
