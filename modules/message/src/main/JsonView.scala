package lila.message

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import scala.concurrent.duration._

import lila.common.PimpedJson._
import lila.user.User
import scala.concurrent.{ Future }
import lila.common.paginator._

final class JsonView() {

  def inbox(me: User, threads: Paginator[Thread]): Result =
    Ok(Json.obj(
      "threads" -> threads.currentPageResults.map { thread => inboxItem(me, thread) }
    ).noNull)

  def inboxItem(me: User, thread: Thread): JsValue =
    Json.obj(
      "id" -> thread.id,
      "author" -> thread.otherUserId(me),
      "name" -> thread.name,
      "updatedAt" -> thread.updatedAt
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
      "sender" -> thread.senderOf(post),
      "receiver" -> thread.receiverOf(post),
      "text" -> post.text,
      "createdAt" -> post.createdAt
    )
}
