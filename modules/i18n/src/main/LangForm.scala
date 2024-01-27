package lila.i18n

import play.api.i18n.Lang
import play.api.data.*
import play.api.data.Forms.*

object LangForm:

  object allLanguages:
    val choices: List[(Language, String)] = LangList.languageChoices
    val mapping: Mapping[Language]        = languageMapping(choices)

  object popularLanguages:
    val choices: List[(Language, String)] = LangList.popularLanguageChoices
    val mapping: Mapping[Language]        = languageMapping(choices)

  private def languageMapping(choices: List[(Language, String)]): Mapping[Language] =
    text.verifying(l => choices.exists(_._1.value == l)).transform(Language(_), _.value)
