package lila.msg

import play.api.libs.json.{ Json, JsValue, JsNumber }
import play.api.mvc.RequestHeader

import lila.db.dsl.{ *, given }
import lila.common.HTTPRequest

final class MsgUnreadCount(colls: MsgColls, cacheApi: lila.memo.CacheApi)(using Executor):

  def mobile(me: Me)(using req: RequestHeader): Fu[JsValue] =
    cache
      .get(me.userId)
      .flatMap: unread =>
        if HTTPRequest.isLichessMobile(req)
        then
          (unread > 0)
            .so(hasLichessMsg(me.userId))
            .map: lichessMsg =>
              Json.obj("unread" -> unread).add("lichess", lichessMsg)
        else fuccess(JsNumber(unread)) // lichobile

  // only hit this when cached unreads > 0
  // or cached notifications count > 0
  def hasLichessMsg(userId: UserId): Fu[Boolean] =
    colls.thread.secondary.exists:
      $id(MsgThread.id(userId, UserId.lichess)) ++ lichessMessageSelect

  private val lichessMessageSelect = $doc(
    "lastMsg.read" -> false,
    "lastMsg.text" -> $doc("$not" -> $doc("$regex" -> "^Welcome!"))
  )

  private val cache = cacheApi[UserId, Int](256, "message.unreadCount"):
    _.expireAfterWrite(10.seconds).buildAsyncFuture[UserId, Int]: userId =>
      colls.thread
        .aggregateOne(_.sec): framework =>
          import framework.*
          Match($doc("users" -> userId, "del".$ne(userId))) -> List(
            Sort(Descending("lastMsg.date")),
            Limit(20),
            Match($doc("lastMsg.read" -> false, "lastMsg.user".$ne(userId))),
            Count("nb")
          )
        .mapz(~_.getAsOpt[Int]("nb"))
