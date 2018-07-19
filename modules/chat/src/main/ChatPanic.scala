package lila.chat

import org.joda.time.DateTime

import lila.user.User

final class ChatPanic {

  private var until: Option[DateTime] = none

  def allowed(u: User, tighter: Boolean): Boolean = !(enabled || tighter) || {
    u.count.gameH > 10 && u.createdSinceDays(1)
  }
  def allowed(u: User): Boolean = allowed(u, false)

  def enabled = until exists { d =>
    (d isAfter DateTime.now) || {
      until = none
      false
    }
  }

  def get = until

  def start = {
    logger.warn("Chat Panic enabled")
    until = DateTime.now.plusMinutes(180).some
  }

  def stop = {
    logger.warn("Chat Panic disabled")
    until = none
  }

  def set(v: Boolean) = if (v) start else stop
}
