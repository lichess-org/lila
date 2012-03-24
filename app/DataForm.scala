package lila.http

import lila.system.model._
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

  val entryGameForm = Form(entryGameMapping)

  type JoinData = (String, String, EntryGame)
  val joinForm = Form(tuple(
    "redirect" -> nonEmptyText,
    "messages" -> nonEmptyText,
    "entry" -> entryGameMapping
  ))

  type RematchData = (String, String, EntryGame)
  val rematchForm = Form(tuple(
    "whiteRedirect" -> nonEmptyText,
    "blackRedirect" -> nonEmptyText,
    "entry" -> entryGameMapping
  ))

  private type MessagesData = String
  private val messagesForm = Form(single(
    "messages" -> nonEmptyText
  ))

  type EndData = MessagesData
  val endForm = messagesForm

  type DrawData = MessagesData
  val drawForm = messagesForm

  private val entryGameMapping = mapping(
    "id" -> nonEmptyText,
    "players" -> list(mapping(
      "u" -> optional(nonEmptyText),
      "ue" -> nonEmptyText
    )(EntryPlayer.apply)(EntryPlayer.unapply)),
    "variant" -> nonEmptyText,
    "rated" -> boolean,
    "clock" -> list(number)
  )(EntryGame.apply)(EntryGame.unapply)
}
