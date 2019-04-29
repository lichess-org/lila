package views.html

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.I18nKeys

object chat {

  val frag = st.section(cls := "mchat")(
    div(cls := "mchat__tabs")(
      div(cls := "mchat__tab")(nbsp)
    ),
    div(cls := "mchat__content")
  )

  import lila.chat.JsonView.chatIdWrites

  def restrictedJson(
    chat: lila.chat.Chat.Restricted,
    name: String,
    timeout: Boolean,
    public: Boolean, // game players chat is not public
    withNote: Boolean = false,
    writeable: Boolean = true,
    localMod: Boolean = false
  )(implicit ctx: Context) =
    json(
      chat.chat, name = name, timeout = timeout, withNote = withNote, writeable = writeable, public = public, restricted = chat.restricted, localMod = localMod
    )

  def json(
    chat: lila.chat.AnyChat,
    name: String,
    timeout: Boolean,
    public: Boolean, // game players chat is not public
    withNote: Boolean = false,
    writeable: Boolean = true,
    restricted: Boolean = false,
    localMod: Boolean = false
  )(implicit ctx: Context) = Json.obj(
    "data" -> Json.obj(
      "id" -> chat.id,
      "name" -> name,
      "lines" -> lila.chat.JsonView(chat),
      "userId" -> ctx.userId,
      "loginRequired" -> chat.loginRequired,
      "restricted" -> restricted
    ),
    "i18n" -> i18n(withNote = withNote),
    "writeable" -> writeable,
    "noteId" -> (withNote && ctx.noBlind).option(chat.id.value take 8),
    "public" -> public,
    "permissions" -> Json.obj("local" -> localMod)
      .add("timeout" -> isGranted(_.ChatTimeout))
      .add("shadowban" -> isGranted(_.Shadowban)),
    "timeout" -> timeout
  ).add("kobold" -> ctx.troll)
    .add("blind" -> ctx.blind)
    .add("timeoutReasons" -> isGranted(_.ChatTimeout).option(lila.chat.JsonView.timeoutReasons))

  def i18n(withNote: Boolean)(implicit ctx: Context) = i18nOptionJsObject(
    I18nKeys.talkInChat.some,
    I18nKeys.toggleTheChat.some,
    I18nKeys.loginToChat.some,
    I18nKeys.youHaveBeenTimedOut.some,
    withNote option I18nKeys.notes,
    withNote option I18nKeys.typePrivateNotesHere
  )
}
