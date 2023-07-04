package lila.chat

import lila.user.User

final class ChatPanic:

  private var until: Option[Instant] = none

  def allowed(u: User): Boolean =
    !enabled || {
      (u.count.gameH > 10 && u.createdSinceDays(1)) || u.isVerified
    }

  def allowed(id: UserId, fetch: UserId => Fu[Option[User]]): Fu[Boolean] =
    if enabled then fetch(id) dmap { _ so allowed }
    else fuTrue

  def enabled =
    until exists { d =>
      d.isAfterNow || {
        until = none
        false
      }
    }

  def get = until

  def start() =
    logger.warn("Chat Panic enabled")
    until = nowInstant.plusMinutes(180).some

  def stop() =
    logger.warn("Chat Panic disabled")
    until = none

  def set(v: Boolean) = if v then start() else stop()
