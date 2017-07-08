package lila.i18n

import java.io._
import scala.concurrent.Future

import play.api.i18n.Lang
import play.api.libs.json.{ JsString, JsObject }

private[i18n] final class JsDump(path: String) {

  def keysToObject(keys: Seq[I18nKey], lang: Lang) = JsObject {
    keys.flatMap { k =>
      Translator.findTranslation(k.key, I18nDb.Site, lang) match {
        case Some(literal: Literal) =>
          List(k.key -> JsString(literal.message))
        case Some(plurals: Plurals) =>
          plurals.messages map {
            case (I18nQuantity.Zero, m) => k.key + ":zero" -> JsString(m)
            case (I18nQuantity.One, m) => k.key + ":one" -> JsString(m)
            case (I18nQuantity.Two, m) => k.key + ":two" -> JsString(m)
            case (I18nQuantity.Few, m) => k.key + ":few" -> JsString(m)
            case (I18nQuantity.Many, m) => k.key + ":many" -> JsString(m)
            case (I18nQuantity.Other, m) => k.key -> JsString(m)
          }
        case None => Nil
      }
    }
  }

  def keysToMessageObject(keys: Seq[I18nKey], lang: Lang) = JsObject {
    keys.map { k =>
      k.literalTxtTo(enLang, Nil) -> JsString(k.literalTxtTo(lang, Nil))
    }
  }

  def apply: Funit = Future {
    pathFile.mkdir
    writeRefs
    writeFullJson
  } void

  private val pathFile = new File(path)

  private def dumpFromKey(keys: Iterable[String], lang: Lang): String =
    keys.map { key =>
      """"%s":"%s"""".format(key, escape(Translator.txt.literal(key, I18nDb.Site, Nil, lang)))
    }.mkString("{", ",", "}")

  private def writeRefs = writeFile(
    new File("%s/refs.json".format(pathFile.getCanonicalPath)),
    LangList.all.toList.sortBy(_._1.code).map {
      case (lang, name) => s"""["${lang.code}","$name"]"""
    }.mkString("[", ",", "]")
  )

  private def writeFullJson = I18nDb.langs foreach { lang =>
    val code = dumpFromKey(I18nDb.site(defaultLang).keys, lang)
    val file = new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.code))
    writeFile(new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.code)), code)
  }

  private def writeFile(file: File, content: String) = {
    val out = new PrintWriter(file)
    try { out.print(content) }
    finally { out.close }
  }

  private def escape(text: String) = text.replace(""""""", """\"""")
}
