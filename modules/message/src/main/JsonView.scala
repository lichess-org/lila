package lila.message

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import lila.common.PimpedJson._
import lila.user.User
import lila.common.paginator._

final class JsonView() {

  def inbox(me: User, threads: Paginator[Thread]): Result =
    Ok(Json.obj(
      "threads" -> PaginatorJson(threads.mapResults { t =>
        Json.obj(
          "id" -> t.id,
          "author" -> t.otherUserId(me),
          "name" -> t.name,
          "updatedAt" -> t.updatedAt,
          "isUnread" -> t.isUnReadBy(me)
        )
      })
    ))

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
      "sender" -> thread.senderOf(post),
      "receiver" -> thread.receiverOf(post),
      "text" -> post.text,
      "createdAt" -> post.createdAt
    )
}
