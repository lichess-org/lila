package lila.message

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

import play.api.data._
import play.api.data.Forms._

import play.twirl.api.Html

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

  private val errNames = Map(
    "error.required" -> "is required",
    "error.minLength" -> "too short",
    "error.maxLength" -> "too long",
    "Unknown username" -> "unknown")

  def createThreadError(form: Form[_]): JsValue =
    Json.obj (
      "err" -> "Malformed request",
      "errors" -> createThreadError(form.errors)
    )

  def createThreadError(errors: Seq[FormError]): JsValue = {
    return errors.foldLeft(JsObject(Seq()))(accErrorMessages);
  }

  def accErrorMessages (acc: JsObject, e: FormError): JsObject = {
    val result = acc \ e.key;

    result match {
      case JsDefined(v: JsArray) => acc + (e.key -> (v :+ JsString(errNames.getOrElse(e.message, e.message))))
      case undefined: JsUndefined => acc + (e.key -> JsArray(Seq(JsString(errNames.getOrElse(e.message, e.message)))))
    }
  }

}
