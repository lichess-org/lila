package lila.i18n

import lila.db.Types.ReactiveColl

import com.typesafe.config.Config
import play.api.i18n.{ MessagesApi, MessagesPlugin }
import play.api.i18n.Lang
import play.api.Play.current

final class Env(
    config: Config,
    db: lila.db.Env,
    val messagesApi: MessagesApi,
    appPath: String) {

  val WebPathRelative = config getString "web_path.relative"
  val FilePathRelative = config getString "file_path.relative"
  val UpstreamUrl = config getString "upstream.url"
  val HideCallsCookieName = config getString "hide_calls.cookie.name"
  val HideCallsCookieMaxAge = config getInt "hide_calls.cookie.max_age"
  val CollectionTranslation = config getString "collection.translation"
  val RequestHandlerProtocol = config getString "request_handler.protocol"

  lazy val translationRepo = new TranslationRepo(db(CollectionTranslation))

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
    repo = translationRepo,
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

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    db = lila.db.Env.current,
    messagesApi = play.api.Play.current.plugin[MessagesPlugin]
      .err("this plugin was not registered or disabled")
      .api,
    appPath = play.api.Play.current.path.getCanonicalPath
  )
}
