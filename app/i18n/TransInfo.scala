package lila
package i18n

import play.api.i18n.{ MessagesApi, Lang }

case class TransInfo(
    lang: Lang,
    name: String,
    contributors: List[String],
    percent: Int) {

  def code = lang.language
}

object TransInfo {

  def all(api: MessagesApi, keys: I18nKeys): List[TransInfo] = {
    val nbMessages = keys.keys.size
    LangList.sortedList.filter(_._1 != "en") map {
      case (code, name) ⇒ TransInfo(
        lang = Lang(code),
        name = name,
        contributors = Contributors(code),
        percent = (api.messages get code).fold(_.size, 0) * 100 / nbMessages
      )
    }
  }
}
