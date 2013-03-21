package lila.i18n

import lila.db.ReactiveColl

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env) {

  val WebPathRelative = config getString "web_path.relative"
  val FilePathRelative = config getString "file_path.relative"
  val UpstreamDomain = config getString "upstream.domain"
  val HideCallsCookieName = config getString "hide_calls.cookie.name"
  val HideCallsCookieMaxAge = config getInt "hide_calls.cookie.max_age"
  val CollectionTranslation = config getString "collection.translation"

  // lazy val translationRepo = new TranslationRepo(db(CollectionTranslation))
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "i18n",
    db = lila.db.Env.current)
}
