package views.html.chat

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.common.PimpedJson._
import lila.i18n.I18nKeys

object ChatJsData {

  def restricted(
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
    "noteId" -> withNote.option(chat.id take 8),
    "public" -> public,
    "kobold" -> ctx.troll,
    "permissions" -> Json.obj(
      "local" -> localMod,
      "timeout" -> isGranted(_.ChatTimeout).option(true),
      "shadowban" -> isGranted(_.MarkTroll).option(true)
    ).noNull,
    "timeout" -> timeout,
    "timeoutReasons" -> isGranted(_.ChatTimeout).option(lila.chat.JsonView.timeoutReasons)
  )

  def i18n(withNote: Boolean)(implicit ctx: Context) = i18nOptionJsObject(
    I18nKeys.talkInChat.some,
    I18nKeys.toggleTheChat.some,
    withNote option I18nKeys.notes,
    withNote option I18nKeys.typePrivateNotesHere
  )
}
