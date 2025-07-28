package lila.chat

import play.api.libs.json.*

import lila.common.Json.given
import lila.ui.*

import ScalatagsTemplate.{ *, given }

object ChatUi:

  val frag: Tag = st.section(cls := "mchat")(
    div(cls := "mchat__tabs")(
      div(cls := "mchat__tab")(nbsp)
    ),
    div(cls := "mchat__content")
  )

  val spectatorsFrag = div(
    cls := "chat__members none",
    aria.live := "off",
    aria.relevant := "additions removals text"
  )

  def restrictedJson(
      chat: Chat.Restricted,
      lines: JsonChatLines,
      name: String,
      timeout: Boolean,
      public: Boolean, // game players chat is not public
      resourceId: Chat.ResourceId,
      withNoteAge: Option[Int] = None,
      writeable: Boolean = true,
      localMod: Boolean = false,
      voiceChat: Boolean = false
  )(using Context): JsObject =
    json(
      chat.chat,
      lines,
      name = name,
      timeout = timeout,
      withNoteAge = withNoteAge,
      writeable = writeable,
      public = public,
      resourceId = resourceId,
      restricted = chat.restricted,
      localMod = localMod,
      voiceChat = voiceChat
    )

  def json(
      chat: AnyChat,
      lines: JsonChatLines,
      name: String,
      timeout: Boolean,
      public: Boolean, // game players chat is not public
      resourceId: Chat.ResourceId,
      withNoteAge: Option[Int] = None,
      writeable: Boolean = true,
      restricted: Boolean = false,
      localMod: Boolean = false,
      broadcastMod: Boolean = false,
      voiceChat: Boolean = false,
      hostIds: List[UserId] = Nil
  )(using ctx: Context): JsObject =
    val noteId = (withNoteAge.isDefined && ctx.noBlind).option(chat.id.value.take(8))
    if ctx.kid.yes then
      Json
        .obj("kidMode" -> true)
        .add("noteId" -> noteId)
        .add("noteAge" -> withNoteAge)
    else
      Json
        .obj(
          "data" -> Json
            .obj(
              "id" -> chat.id,
              "name" -> name,
              "lines" -> lines,
              "resourceId" -> resourceId.value
            )
            .add("hostIds" -> hostIds.some.filter(_.nonEmpty))
            .add("userId" -> ctx.userId)
            .add("loginRequired" -> chat.loginRequired)
            .add("restricted" -> restricted)
            .add("voiceChat" -> (voiceChat && ctx.isAuth)),
          "writeable" -> writeable,
          "public" -> public,
          "permissions" -> Json
            .obj("local" -> (public && localMod))
            .add("broadcast" -> (public && broadcastMod))
            .add("timeout" -> (public && Granter.opt(_.ChatTimeout)))
            .add("shadowban" -> (public && Granter.opt(_.Shadowban)))
        )
        .add("kobold" -> ctx.troll)
        .add("blind" -> ctx.blind)
        .add("timeout" -> timeout)
        .add("noteId" -> noteId)
        .add("noteAge" -> withNoteAge)
        .add(
          "timeoutReasons" -> (!localMod && (Granter.opt(_.ChatTimeout) || Granter.opt(_.BroadcastTimeout)))
            .option(JsonView.timeoutReasons)
        )
