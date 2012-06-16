package lila
package i18n

import play.api.Application
import play.api.i18n.Lang
import play.api.i18n.MessagesPlugin
import com.mongodb.casbah.MongoCollection

import core.Settings

final class I18nEnv(
    app: Application,
    mongodb: String ⇒ MongoCollection,
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
    keys = keys,
    api = messagesApi)

  lazy val transInfos = TransInfos(
    api = messagesApi,
    keys = keys)

  lazy val translationRepo = new TranslationRepo(mongodb(MongoCollectionTranslation))

  lazy val forms = new DataForm(
    repo = translationRepo,
    keys = keys)

  lazy val upstreamFetch = new UpstreamFetch(
    upstreamDomain = I18nUpstreamDomain)

  lazy val gitWrite = new GitWrite(
    transRelPath = I18nFilePathRelative,
    repoPath = app.path.getCanonicalPath)

  def hideCallsCookieName = I18nHideCallsCookieName
}
