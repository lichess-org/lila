package lila.i18n

import lila.common.PlayApp

import com.typesafe.config.Config
import play.api.i18n.{ MessagesApi, MessagesPlugin }
import play.api.i18n.Lang
import play.api.Play.current

final class Env(
    config: Config,
    db: lila.db.Env,
    val messagesApi: MessagesApi,
    appPath: String) {

  private val settings = new {
    val WebPathRelative = config getString "web_path.relative"
    val FilePathRelative = config getString "file_path.relative"
    val UpstreamUrl = config getString "upstream.url"
    val HideCallsCookieName = config getString "hide_calls.cookie.name"
    val HideCallsCookieMaxAge = config getInt "hide_calls.cookie.max_age"
    val CollectionTranslation = config getString "collection.translation"
  }
  import settings._

  // public settings
  val RequestHandlerProtocol = config getString "request_handler.protocol"

  private[i18n] lazy val translationColl = db(CollectionTranslation)

  lazy val pool = new I18nPool(
    langs = Lang.availables.toSet,
    default = Lang("en"))

  lazy val translator = new Translator(
    api = messagesApi,
    pool = pool)

  lazy val keys = new I18nKeys(translator)

  lazy val requestHandler = new I18nRequestHandler(pool, RequestHandlerProtocol)

  lazy val jsDump = new JsDump(
    path = appPath + "/" + WebPathRelative,
    pool = pool,
    keys = keys)

  lazy val fileFix = new FileFix(
    path = appPath + "/" + FilePathRelative,
    pool = pool,
    keys = keys,
    api = messagesApi)

  lazy val transInfos = TransInfos(
    api = messagesApi,
    keys = keys)

  lazy val forms = new DataForm(
    keys = keys /*, captcher = captcha*/ )

  def upstreamFetch(makeUrl: Int ⇒ String) =
    new UpstreamFetch(id ⇒ UpstreamUrl + makeUrl(id))

  lazy val gitWrite = new GitWrite(
    transRelPath = FilePathRelative,
    repoPath = appPath)

  def hideCallsCookieName = HideCallsCookieName
  def hideCallsCookieMaxAge = HideCallsCookieMaxAge
}

object Env {

  lazy val current = "[i18n] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    db = lila.db.Env.current,
    messagesApi = PlayApp.withApp(_.plugin[MessagesPlugin])
      .err("this plugin was not registered or disabled")
      .api,
    appPath = PlayApp withApp (_.path.getCanonicalPath)
  )
}
