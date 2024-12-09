package lila.core
package msg

import lila.core.userId.UserId

enum PostResult:
  case Success, Invalid, Limited, Bounced

case class MsgPreset(name: String, text: String)

case class SystemMsg(userId: UserId, text: String)

type ID = String

case class IdText(id: String, text: String)

trait MsgApi:
  def postPreset(destId: UserId, preset: MsgPreset): Fu[PostResult]
  def post(
      orig: UserId,
      dest: UserId,
      text: String,
      multi: Boolean = false,
      date: Instant = nowInstant,
      ignoreSecurity: Boolean = false
  ): Fu[PostResult]
