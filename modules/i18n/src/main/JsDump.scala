package lila.i18n

import java.io._
import scala.concurrent.Future

import play.api.i18n.Lang
import play.api.libs.json.{ JsString, JsObject }

private[i18n] final class JsDump(path: String) {

  def keysToObject(keys: Seq[I18nKey], lang: Lang) = JsObject {
    keys.map { k =>
      k.key -> JsString(k.to(lang)())
    }
  }

  def keysToMessageObject(keys: Seq[I18nKey], lang: Lang) = JsObject {
    keys.map { k =>
      k.en() -> JsString(k.to(lang)())
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
      """"%s":"%s"""".format(key, escape(Translator.str(key, Nil, lang)))
    }.mkString("{", ",", "}")

  private def writeRefs {
    val code = LangList.all.toList.sortBy(_._1.toString).map {
      case (code, name) => s"""["$code","$name"]"""
    }.mkString("[", ",", "]")
    val file = new File("%s/refs.json".format(pathFile.getCanonicalPath))
    val out = new PrintWriter(file)
    try { out.print(code) }
    finally { out.close }
  }

  private def writeFullJson {
    val keys = I18nDb.all(defaultLang).keys
    I18nDb.langs foreach { lang =>
      val code = dumpFromKey(keys, lang)
      val file = new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.language))
      val out = new PrintWriter(file)
      try { out.print(code) }
      finally { out.close }
    }
  }

  private def escape(text: String) = text.replace(""""""", """\"""")
}
