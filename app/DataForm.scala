package lila.http

import play.api.data._
import play.api.data.Forms._

import lila.system.model.Hook

object DataForm {

  type MoveData = (String, String, Option[String], Option[Int])
  val moveForm = Form(tuple(
    "from" -> nonEmptyText,
    "to" -> nonEmptyText,
    "promotion" -> optional(text),
    "b" -> optional(number)
  ))

  type TalkData = (String, String)
  val talkForm = Form(tuple(
    "author" -> nonEmptyText,
    "message" -> nonEmptyText
  ))

  type JoinData = (String, String)
  val joinForm = Form(tuple(
    "redirect" -> nonEmptyText,
    "messages" -> nonEmptyText
  ))

  type RematchData = (String, String)
  val rematchForm = Form(tuple(
    "whiteRedirect" -> nonEmptyText,
    "blackRedirect" -> nonEmptyText
  ))

  private type MessagesData = String
  private val messagesForm = Form(single(
    "messages" -> nonEmptyText
  ))

  type EndData = MessagesData
  val endForm = messagesForm

  type DrawData = MessagesData
  val drawForm = messagesForm

  val hookForm = Form(mapping(
    "id" -> nonEmptyText,
    "ownerId" -> nonEmptyText,
    "variant" -> number,
    "time" -> optional(number),
    "increment" -> optional(number),
    "mode" -> number,
    "color" -> nonEmptyText,
    "username" -> text,
    "elo" -> optional(number),
    "match" -> boolean,
    "eloRange" -> optional(text),
    "engine" -> boolean
  )(Hook.apply)(Hook.unapply))
}
