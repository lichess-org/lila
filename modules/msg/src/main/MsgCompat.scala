package lila.msg

import play.api.libs.json._

import lila.common.Json.jodaWrites
import lila.common.LightUser
import lila.common.config._
import lila.common.paginator._
import lila.user.{ LightUserApi, User }

final class MsgCompat(
    api: MsgApi,
    isOnline: lila.socket.IsOnline,
    lightUserApi: LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val maxPerPage = MaxPerPage(25)

  def inbox(me: User, pageOpt: Option[Int]): Fu[JsObject] = {
    val page = pageOpt.fold(1)(_ atLeast 1 atMost 2)
    api.threadsOf(me) flatMap { allThreads =>
      val threads = allThreads.drop((page - 1) * maxPerPage.value).take(maxPerPage.value)
      lightUserApi.preloadMany(threads.map(_ other me)) inject
        PaginatorJson {
          Paginator
            .fromResults(
              currentPageResults = threads,
              nbResults = allThreads.size,
              currentPage = page,
              maxPerPage = maxPerPage
            )
            .mapResults { t =>
              val user = lightUserApi.sync(t other me) | LightUser.fallback(t other me)
              Json.obj(
                "id"        -> user.id,
                "author"    -> user.titleName,
                "name"      -> t.lastMsg.text,
                "updatedAt" -> t.lastMsg.date,
                "isUnread"  -> t.lastMsg.unreadBy(me.id)
              )
            }
        }
    }
  }

  def thread(me: User, c: MsgConvo): JsObject =
    Json.obj(
      "id"   -> c.contact.id,
      "name" -> c.contact.name,
      "posts" -> c.msgs.map { msg =>
        Json.obj(
          "sender"    -> renderUser(if (msg.user == c.contact.id) c.contact else me.light),
          "receiver"  -> renderUser(if (msg.user != c.contact.id) c.contact else me.light),
          "text"      -> msg.text,
          "createdAt" -> msg.date
        )
      }
    )

  private def renderUser(user: LightUser) =
    LightUser.lightUserWrites.writes(user) ++ Json.obj(
      "online"   -> isOnline(user.id),
      "username" -> user.name // for mobile app BC
    )
}
