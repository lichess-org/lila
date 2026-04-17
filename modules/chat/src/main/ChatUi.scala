package lila.chat

import play.api.libs.json.*

import lila.common.Json.given
import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.core.chat.PublicSource

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
      resource: PublicSource,
      withNoteAge: Option[Int] = None,
      writeable: Boolean = true,
      localMod: Boolean = false,
      voiceChat: Boolean = false,
      opponentId: Option[UserId] = None
  )(using Context): JsObject =
    json(
      chat.chat,
      lines,
      name = name,
      timeout = timeout,
      withNoteAge = withNoteAge,
      writeable = writeable,
      public = public,
      resource = resource,
      restricted = chat.restricted,
      localMod = localMod,
      voiceChat = voiceChat,
      opponentId = opponentId
    )

  def json(
      chat: AnyChat,
      lines: JsonChatLines,
      name: String,
      timeout: Boolean,
      public: Boolean, // game players chat is not public
      resource: PublicSource,
      withNoteAge: Option[Int] = None,
      writeable: Boolean = true,
      restricted: Boolean = false,
      localMod: Boolean = false,
      broadcastMod: Boolean = false,
      voiceChat: Boolean = false,
      hostIds: List[UserId] = Nil,
      opponentId: Option[UserId] = None
  )(using ctx: Context): JsObject =
    val noteId = (withNoteAge.isDefined && ctx.noBlind).option(chat.id.value.take(8))
    Json
      .obj(
        "data" -> Json
          .obj(
            "id" -> chat.id,
            "name" -> name,
            "lines" -> (if ctx.kid.no then lines else Json.arr()),
            "resourceType" -> resource.typeName,
            "resourceId" -> resource.resourceId
          )
          .add("hostIds" -> hostIds.nonEmptyOption)
          .add("userId" -> ctx.userId)
          .add("loginRequired" -> chat.loginRequired)
          .add("restricted" -> restricted)
          .add("voiceChat" -> (voiceChat && ctx.isAuth))
          .add("opponentId" -> opponentId),
        "writeable" -> writeable,
        "public" -> public,
        "permissions" -> Json
          .obj("local" -> (public && localMod))
          .add("broadcast" -> (public && broadcastMod))
          .add("timeout" -> (public && Granter.opt(_.ChatTimeout)))
          .add("shadowban" -> (public && Granter.opt(_.Shadowban)))
      )
      .add("kidMode" -> ctx.kid)
      .add("kobold" -> ctx.troll)
      .add("blind" -> ctx.blind)
      .add("timeout" -> timeout)
      .add("noteId" -> noteId)
      .add("noteAge" -> withNoteAge)
      .add(
        "timeoutReasons" -> (!localMod && (Granter.opt(_.ChatTimeout) || Granter.opt(_.BroadcastTimeout)))
          .option(ChatJsonView.timeoutReasons)
      )
