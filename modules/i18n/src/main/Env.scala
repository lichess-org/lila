package lila.i18n

import com.typesafe.config.Config
import play.api.i18n.Lang
import play.api.libs.json._

final class Env(
    config: Config,
    db: lila.db.Env,
    system: akka.actor.ActorSystem,
    messages: Messages,
    captcher: akka.actor.ActorSelection,
    appPath: String) {

  private val settings = new {
    val WebPathRelative = config getString "web_path.relative"
    val FilePathRelative = config getString "file_path.relative"
    val UpstreamUrlPattern = config getString "upstream.url_pattern"
    val HideCallsCookieName = config getString "hide_calls.cookie.name"
    val HideCallsCookieMaxAge = config getInt "hide_calls.cookie.max_age"
    val CollectionTranslation = config getString "collection.translation"
    val ContextGitUrl = config getString "context.git.url"
    val ContextGitFile = config getString "context.git.file"
    val CdnDomain = config getString "cdn_domain"
    val CallThreshold = config getInt "call.threshold"
  }
  import settings._

  // public settings
  val RequestHandlerProtocol = config getString "request_handler.protocol"
  def hideCallsCookieName = HideCallsCookieName
  def hideCallsCookieMaxAge = HideCallsCookieMaxAge

  private val translationColl = db(CollectionTranslation)

  lazy val pool = new I18nPool(
    langs = Lang.availables(play.api.Play.current).toSet,
    default = I18nKey.en)

  lazy val translator = new Translator(
    messages = messages,
    pool = pool)

  lazy val keys = new I18nKeys(translator)

  lazy val requestHandler = new I18nRequestHandler(
    pool,
    RequestHandlerProtocol,
    CdnDomain)

  lazy val jsDump = new JsDump(
    path = appPath + "/" + WebPathRelative,
    pool = pool,
    keys = keys)

  lazy val fileFix = new FileFix(
    path = appPath + "/" + FilePathRelative,
    pool = pool,
    keys = keys,
    messages = messages)

  lazy val transInfos = TransInfos(
    messages = messages,
    keys = keys)

  lazy val repo = new TranslationRepo(translationColl)

  lazy val forms = new DataForm(
    repo = repo,
    keys = keys,
    captcher = captcher,
    callApi = callApi)

  def upstreamFetch = new UpstreamFetch(id => UpstreamUrlPattern format id)

  lazy val gitWrite = new GitWrite(
    transRelPath = FilePathRelative,
    repoPath = appPath,
    system = system)

  lazy val context = new Context(ContextGitUrl, ContextGitFile, keys)

  private lazy val callApi = new CallApi(
    hideCallsCookieName = hideCallsCookieName,
    minGames = CallThreshold,
    transInfos = transInfos)

  def call = callApi.apply _

  def jsonFromVersion(v: Int): Fu[JsValue] = {
    repo findFrom v map { ts => Json toJson ts }
  }

  def cli = new lila.common.Cli {
    def process = {
      case "i18n" :: "fetch" :: from :: Nil =>
        upstreamFetch(from) flatMap gitWrite.apply inject "Fetched translations from upstream"
      case "i18n" :: "js" :: "dump" :: Nil =>
        jsDump.apply inject "Dumped JavaScript translations"
      case "i18n" :: "file" :: "fix" :: Nil =>
        fileFix.apply inject "Fixed translation files"
    }
  }
}

object Env {

  import lila.common.PlayApp

  lazy val current = "i18n" boot new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    db = lila.db.Env.current,
    system = PlayApp.system,
    messages = MessageDb.load,
    captcher = lila.hub.Env.current.actor.captcher,
    appPath = PlayApp withApp (_.path.getCanonicalPath)
  )
}
