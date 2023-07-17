package views.html

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.i18n.I18nKeys
import lila.common.Json.given

object chat:

  val frag = st.section(cls := "mchat")(
    div(cls := "mchat__tabs")(
      div(cls := "mchat__tab")(nbsp)
    ),
    div(cls := "mchat__content")
  )

  def restrictedJson(
      chat: lila.chat.Chat.Restricted,
      name: String,
      timeout: Boolean,
      public: Boolean, // game players chat is not public
      resourceId: lila.chat.Chat.ResourceId,
      withNoteAge: Option[Int] = None,
      writeable: Boolean = true,
      localMod: Boolean = false,
      palantir: Boolean = false
  )(using Context) =
    json(
      chat.chat,
      name = name,
      timeout = timeout,
      withNoteAge = withNoteAge,
      writeable = writeable,
      public = public,
      resourceId = resourceId,
      restricted = chat.restricted,
      localMod = localMod,
      palantir = palantir
    )

  def json(
      chat: lila.chat.AnyChat,
      name: String,
      timeout: Boolean,
      public: Boolean, // game players chat is not public
      resourceId: lila.chat.Chat.ResourceId,
      withNoteAge: Option[Int] = None,
      writeable: Boolean = true,
      restricted: Boolean = false,
      localMod: Boolean = false,
      broadcastMod: Boolean = false,
      palantir: Boolean = false
  )(using ctx: Context) =
    Json
      .obj(
        "data" -> Json
          .obj(
            "id"         -> chat.id,
            "name"       -> name,
            "lines"      -> lila.chat.JsonView(chat),
            "userId"     -> ctx.userId,
            "resourceId" -> resourceId.value
          )
          .add("loginRequired" -> chat.loginRequired)
          .add("restricted" -> restricted)
          .add("palantir" -> (palantir && ctx.isAuth)),
        "i18n"      -> i18n(withNote = withNoteAge.isDefined),
        "writeable" -> writeable,
        "public"    -> public,
        "permissions" -> Json
          .obj("local" -> (public && localMod))
          .add("broadcast" -> (public && broadcastMod))
          .add("timeout" -> (public && isGranted(_.ChatTimeout)))
          .add("shadowban" -> (public && isGranted(_.Shadowban)))
      )
      .add("kobold" -> ctx.troll)
      .add("blind" -> ctx.blind)
      .add("timeout" -> timeout)
      .add("noteId" -> (withNoteAge.isDefined && ctx.noBlind).option(chat.id.value take 8))
      .add("noteAge" -> withNoteAge)
      .add(
        "timeoutReasons" -> (!localMod && (isGranted(_.ChatTimeout) || isGranted(_.BroadcastTimeout)))
          .option(lila.chat.JsonView.timeoutReasons)
      )

  def i18n(withNote: Boolean)(using Context) =
    i18nOptionJsObject(
      I18nKeys.talkInChat.some,
      I18nKeys.toggleTheChat.some,
      I18nKeys.loginToChat.some,
      I18nKeys.youHaveBeenTimedOut.some,
      withNote option I18nKeys.notes,
      withNote option I18nKeys.typePrivateNotesHere
    )

  val spectatorsFrag =
    div(
      cls           := "chat__members none",
      aria.live     := "off",
      aria.relevant := "additions removals text"
    )
