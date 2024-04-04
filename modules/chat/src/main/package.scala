package lila.chat

import play.api.libs.json.JsObject

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*
export lila.core.chat.{ Line, ChatLine, BusChan }

private val logger = lila.log("chat")

case class RoundLine(line: Line, json: JsObject, watcher: Boolean)
case class Timeout(chatId: ChatId, mod: UserId, userId: UserId, reason: ChatTimeout.Reason, local: Boolean)

opaque type AllMessages = Boolean
object AllMessages extends YesNo[AllMessages]
