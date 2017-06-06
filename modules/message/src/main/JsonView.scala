package lila.message

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import lila.common.LightUser
import lila.common.paginator._
import lila.user.User

final class JsonView(
    isOnline: lila.user.User.ID => Boolean,
    lightUser: LightUser.GetterSync
) {

  def inbox(me: User, threads: Paginator[Thread]): Result =
    Ok(PaginatorJson(threads.mapResults { t =>
      Json.obj(
        "id" -> t.id,
        "author" -> t.visibleOtherUserId(me),
        "name" -> t.name,
        "updatedAt" -> t.updatedAt,
        "isUnread" -> t.isUnReadBy(me)
      )
    }))

  def thread(thread: Thread): Fu[JsValue] =
    fuccess(
      Json.obj(
        "id" -> thread.id,
        "name" -> thread.name,
        "posts" -> thread.posts.map { post => threadPost(thread, post) }
      )
    )

  def threadPost(thread: Thread, post: Post): JsValue =
    Json.obj(
      "sender" -> user(thread.visibleSenderOf(post)),
      "receiver" -> user(thread.visibleReceiverOf(post)),
      "text" -> post.text,
      "createdAt" -> post.createdAt
    )

  private def user(userId: String) =
    lightUser(userId).map { l =>
      LightUser.lightUserWrites.writes(l) ++ Json.obj(
        "online" -> isOnline(userId),
        "username" -> l.name // for mobile app BC
      )
    }
}
