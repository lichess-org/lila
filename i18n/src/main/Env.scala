package lila.i18n

import lila.common.PlayApp

import akka.actor.ActorRef
import com.typesafe.config.Config
import play.api.i18n.{ MessagesApi, MessagesPlugin }
import play.api.i18n.Lang
import play.api.Play.current

final class Env(
    config: Config,
    db: lila.db.Env,
    val messagesApi: MessagesApi,
    captcher: ActorRef,
    appPath: String) {

  private val settings = new {
    val WebPathRelative = config getString "web_path.relative"
    val FilePathRelative = config getString "file_path.relative"
    val UpstreamUrlPattern = config getString "upstream.url_pattern"
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
    keys = keys, 
    captcher = captcher)

  def upstreamFetch = new UpstreamFetch(id ⇒ UpstreamUrlPattern format id)

  lazy val gitWrite = new GitWrite(
    transRelPath = FilePathRelative,
    repoPath = appPath)

  def hideCallsCookieName = HideCallsCookieName
  def hideCallsCookieMaxAge = HideCallsCookieMaxAge

  def cli = new lila.common.Cli {
    import play.api.libs.concurrent.Execution.Implicits._
    def process = {
      case "i18n" :: "fetch" :: from :: Nil ⇒
        upstreamFetch(from) flatMap gitWrite.apply inject "Fetched translations from upstream"
      case "i18n" :: "js-dump" :: Nil ⇒
        jsDump.apply inject "Dumped JavaScript translations"
      case "i18n" :: "file-fix" :: Nil ⇒
        fileFix.apply inject "Fixed translation files"
    }
  }
}

object Env {

  lazy val current = "[i18n] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    db = lila.db.Env.current,
    messagesApi = PlayApp.withApp(_.plugin[MessagesPlugin])
      .err("this plugin was not registered or disabled")
      .api,
    captcher = lila.hub.Env.current.actor.captcher,
    appPath = PlayApp withApp (_.path.getCanonicalPath)
  )
}
