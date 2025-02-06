package lila.i18n

import play.api.data.*
import play.api.data.Forms.*

import scalalib.model.Language

object LangForm:

  val select = Form(single("lang" -> text.verifying(code => LangPicker.byStr(code).isDefined)))

  object allLanguages:
    val choices: List[(Language, String)] = LangList.languageChoices
    val mapping: Mapping[Language]        = languageMapping(choices)

  object popularLanguages:
    val choices: List[(Language, String)] = LangList.popularLanguageChoices
    val mapping: Mapping[Language]        = languageMapping(choices)

  private def languageMapping(choices: List[(Language, String)]): Mapping[Language] =
    text.verifying(l => choices.exists(_._1.value == l)).transform(Language(_), _.value)
