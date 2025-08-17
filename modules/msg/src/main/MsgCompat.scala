package lila.msg

import play.api.data.*
import play.api.data.Forms.*
import play.api.libs.json.*
import scalalib.Json.given
import scalalib.paginator.*

import lila.common.Json.given
import lila.core.LightUser

final class MsgCompat(
    api: MsgApi,
    security: MsgSecurity,
    isOnline: lila.core.socket.IsOnline,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor):

  private val maxPerPage = MaxPerPage(25)

  def inbox(pageOpt: Option[Int])(using me: Me): Fu[JsObject] =
    val page = pageOpt.fold(1)(_.atLeast(1).atMost(2))
    api.myThreads.flatMap: allThreads =>
      val threads =
        allThreads.slice((page - 1) * maxPerPage.value, (page - 1) * maxPerPage.value + maxPerPage.value)
      lightUserApi
        .preloadMany(threads.map(_.other(me)))
        .inject(Json.toJsObject:
          Paginator
            .fromResults(
              currentPageResults = threads,
              nbResults = allThreads.size,
              currentPage = page,
              maxPerPage = maxPerPage
            )
            .mapResults: t =>
              val user = lightUserApi.syncFallback(t.other(me))
              Json.obj(
                "id" -> user.id,
                "author" -> user.titleName,
                "name" -> t.lastMsg.text,
                "updatedAt" -> t.lastMsg.date,
                "isUnread" -> t.lastMsg.unreadBy(me)
              ))

  def thread(c: MsgConvo)(using me: Me): JsObject =
    Json.obj(
      "id" -> c.contact.id,
      "name" -> c.contact.name,
      "posts" -> c.msgs.reverse.map: msg =>
        Json.obj(
          "sender" -> renderUser(if msg.user == c.contact.id then c.contact else me.light),
          "receiver" -> renderUser(if msg.user != c.contact.id then c.contact else me.light),
          "text" -> msg.text,
          "createdAt" -> msg.date
        )
    )

  def create(using play.api.mvc.Request[?], FormBinding)(using me: Me): Either[Form[?], Fu[UserId]] =
    Form(
      mapping(
        "username" -> lila.common.Form.username.historicalField
          .verifying("Unknown username", { blockingFetchUser(_).isDefined })
          .verifying(
            "Sorry, this player doesn't accept new messages",
            name =>
              security.may
                .post(me, name.id, isNew = true)
                .await(2.seconds, "pmAccept") // damn you blocking API
          ),
        "subject" -> text(minLength = 3, maxLength = 100),
        "text" -> text(minLength = 3, maxLength = 8000)
      )(ThreadData.apply)(unapply)
    ).bindFromRequest()
      .fold(
        err => Left(err),
        data => Right(api.post(me, data.user.id, s"${data.subject}\n${data.text}").inject(data.user.id))
      )

  def reply(userId: UserId)(using
      play.api.mvc.Request[?],
      FormBinding
  )(using me: Me): Either[Form[?], Funit] =
    Form(single("text" -> text(minLength = 3)))
      .bindFromRequest()
      .fold(
        err => Left(err),
        text => Right(api.post(me, userId, text).void)
      )

  private def blockingFetchUser(username: UserStr) =
    lightUserApi.async(username.id).await(500.millis, "pmUser")

  private case class ThreadData(user: UserStr, subject: String, text: String)

  private def renderUser(user: LightUser) =
    Json.toJsObject(user) ++ Json.obj(
      "online" -> isOnline.exec(user.id),
      "username" -> user.name // for mobile app BC
    )
