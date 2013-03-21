package lila.i18n

import lila.db.ReactiveColl

import com.typesafe.config.Config
import play.api.i18n.{ MessagesApi, MessagesPlugin }
import play.api.i18n.Lang
import play.api.Play.current

final class Env(
    config: Config,
    db: lila.db.Env,
    val messagesApi: MessagesApi) {

  val WebPathRelative = config getString "web_path.relative"
  val FilePathRelative = config getString "file_path.relative"
  val UpstreamDomain = config getString "upstream.domain"
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
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    db = lila.db.Env.current,
    messagesApi = play.api.Play.current.plugin[MessagesPlugin]
      .err("this plugin was not registered or disabled")
      .api
  )
}
