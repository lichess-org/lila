package lila.core
package msg

import lila.core.userId.UserId
import lila.core.data.Url

enum PostResult:
  case Success, Invalid, Limited, Bounced

case class MsgPreset(name: String, text: String)

case class SystemMsg(userId: UserId, text: String)

case class PayoutMessage(userId: UserId, tournamentName: String, tournamentUrl: Url, finishedAt: Instant)

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
  def systemPost(destId: UserId, text: String): Fu[PostResult]
