package lila
package i18n

import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.MessagesPlugin

import core.Settings

final class I18nEnv(
    app: Application,
    settings: Settings) {

  implicit val ctx = app
  import settings._

  lazy val messagesApi = app.plugin[MessagesPlugin]
    .err("this plugin was not registered or disabled")
    .api

  lazy val pool = new I18nPool(
    langs = Lang.availables.toSet,
    default = Lang("en"))

  lazy val translator = new Translator(
    api = messagesApi,
    pool = pool)

  lazy val keys = new I18nKeys(translator = translator)

  lazy val requestHandler = new I18nRequestHandler(pool = pool)

  lazy val jsDump = new JsDump(
    path = app.path.getCanonicalPath + "/" + I18nWebPathRelative,
    pool = pool,
    keys = keys)

  lazy val fileFix = new FileFix(
    path = app.path.getCanonicalPath + "/" + I18nFilePathRelative,
    pool = pool,
    keys = keys)
}
