package lila.i18n

import play.api.i18n.Lang

case class TransInfo(
    lang: Lang,
    name: String,
    contributors: List[String],
    nbTranslated: Int,
    nbMissing: Int
) {

  def code = lang.language

  def codeAndName = code + " - " + name

  def nbMessages = nbTranslated + nbMissing

  def percent = nbTranslated * 100 / nbMessages

  def complete = percent == 100

  def nonComplete = !complete
}

private[i18n] case class TransInfos(all: List[TransInfo]) {

  lazy val byCode = all map { info =>
    info.code -> info
  } toMap

  def get(code: String): Option[TransInfo] = byCode get code

  def get(lang: Lang): Option[TransInfo] = get(lang.language)
}

private[i18n] object TransInfos {

  val defaultCode = "en"

  def apply(messages: Messages, keys: I18nKeys): TransInfos = TransInfos {
    val nbMessages = keys.count
    LangList.sortedList.filter(_._1 != defaultCode) map {
      case (code, name) => TransInfo(
        lang = Lang(code),
        name = name,
        contributors = Contributors(code),
        nbTranslated = messages.get(code) ?? (_.size),
        nbMissing = nbMessages - (messages.get(code) ?? (_.size))
      )
    }
  }
}
