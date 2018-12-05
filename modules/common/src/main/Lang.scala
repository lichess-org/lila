package lila.common

import play.api.i18n.{ Lang => PlayLang }

/* Play has an implicit Lang that is made available every-fucking-where
 * by the fact that it comes from its companion object.
 * Therefore we can't trust any implicit Lang value.
 * This type works around that */
case class Lang(value: PlayLang) extends AnyVal {

  def language = value.language
  def code = value.code
  def toLocale = value.toLocale

  def is(other: Lang) = other.code == code
}

object Lang {

  def apply(language: String): Lang = Lang(PlayLang(language))
  def apply(language: String, country: String): Lang = Lang(PlayLang(language, country))

  def get(code: String): Option[Lang] = PlayLang get code map apply
}
