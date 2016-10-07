package lila.message

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import play.api.data._
import play.api.data.Forms._

import lila.common.PimpedJson._
import lila.user.User
import lila.common.paginator._

final class JsonView(
    isOnline: lila.user.User.ID => Boolean
    ) {

  def inbox(me: User, threads: Paginator[Thread]): Result =
    Ok(PaginatorJson(threads.mapResults { t =>
        Json.obj(
          "id" -> t.id,
          "author" -> t.otherUserId(me),
          "name" -> t.name,
          "updatedAt" -> t.updatedAt,
          "isUnread" -> t.isUnReadBy(me)
        )
      })
    )

  def thread(thread: Thread): Fu[JsValue] =
    fuccess (
      Json.obj(
        "id" -> thread.id,
        "name" -> thread.name,
        "posts" -> thread.posts.map { post => threadPost(thread, post)}
      )
    )

  def threadPost(thread: Thread, post: Post): JsValue =
    Json.obj(
      "sender" -> user(thread.senderOf(post)),
      "receiver" -> user(thread.receiverOf(post)),
      "text" -> post.text,
      "createdAt" -> post.createdAt
    )

  def user(userId: String): JsValue =
    Json.obj(
      "username" -> userId,
      "online" -> isOnline(userId)
    )
}
