package lila.msg

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*
import reactivemongo.api.ReadPreference
import scala.concurrent.duration.*

import lila.common.config.*
import lila.common.Json.given
import lila.common.LightUser
import lila.common.paginator.*
import lila.db.dsl.{ *, given }
import lila.user.{ LightUserApi, User }

final class MsgCompat(
    api: MsgApi,
    colls: MsgColls,
    security: MsgSecurity,
    cacheApi: lila.memo.CacheApi,
    isOnline: lila.socket.IsOnline,
    lightUserApi: LightUserApi
)(using ec: scala.concurrent.ExecutionContext):

  private val maxPerPage = MaxPerPage(25)

  def inbox(me: User, pageOpt: Option[Int]): Fu[JsObject] =
    val page = pageOpt.fold(1)(_ atLeast 1 atMost 2)
    api.threadsOf(me) flatMap { allThreads =>
      val threads =
        allThreads.slice((page - 1) * maxPerPage.value, (page - 1) * maxPerPage.value + maxPerPage.value)
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
              val user = lightUserApi.syncFallback(t other me)
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

  def unreadCount(me: User): Fu[Int] = unreadCountCache.get(me.id)

  private val unreadCountCache = cacheApi[UserId, Int](256, "message.unreadCount") {
    _.expireAfterWrite(10 seconds)
      .buildAsyncFuture[UserId, Int] { userId =>
        colls.thread
          .aggregateOne(ReadPreference.secondaryPreferred) { framework =>
            import framework.*
            Match($doc("users" -> userId, "del" $ne userId)) -> List(
              Sort(Descending("lastMsg.date")),
              Limit(maxPerPage.value),
              Match($doc("lastMsg.read" -> false, "lastMsg.user" $ne userId)),
              Count("nb")
            )
          }
          .map(~_.flatMap(_.getAsOpt[Int]("nb")))
      }
  }

  def thread(me: User, c: MsgConvo): JsObject =
    Json.obj(
      "id"   -> c.contact.id,
      "name" -> c.contact.name,
      "posts" -> c.msgs.reverse.map { msg =>
        Json.obj(
          "sender"    -> renderUser(if (msg.user == c.contact.id) c.contact else me.light),
          "receiver"  -> renderUser(if (msg.user != c.contact.id) c.contact else me.light),
          "text"      -> msg.text,
          "createdAt" -> msg.date
        )
      }
    )

  def create(
      me: User
  )(implicit req: play.api.mvc.Request[?], formBinding: FormBinding): Either[Form[?], Fu[UserId]] =
    Form(
      mapping(
        "username" -> lila.user.UserForm.historicalUsernameField
          .verifying("Unknown username", { blockingFetchUser(_).isDefined })
          .verifying(
            "Sorry, this player doesn't accept new messages",
            { name =>
              security.may
                .post(me.id, name.id, isNew = true)
                .await(2 seconds, "pmAccept") // damn you blocking API
            }
          ),
        "subject" -> text(minLength = 3, maxLength = 100),
        "text"    -> text(minLength = 3, maxLength = 8000)
      )(ThreadData.apply)(unapply)
    ).bindFromRequest()
      .fold(
        err => Left(err),
        data => Right(api.post(me.id, data.user.id, s"${data.subject}\n${data.text}") inject data.user.id)
      )

  def reply(me: User, userId: UserId)(using
      req: play.api.mvc.Request[?],
      formBinding: FormBinding
  ): Either[Form[?], Funit] =
    Form(single("text" -> text(minLength = 3)))
      .bindFromRequest()
      .fold(
        err => Left(err),
        text => Right(api.post(me.id, userId, text).void)
      )

  private def blockingFetchUser(username: UserStr) =
    lightUserApi.async(username.id).await(500 millis, "pmUser")

  private case class ThreadData(user: UserStr, subject: String, text: String)

  private def renderUser(user: LightUser) =
    LightUser.lightUserWrites.writes(user) ++ Json.obj(
      "online"   -> isOnline.value(user.id),
      "username" -> user.name // for mobile app BC
    )
