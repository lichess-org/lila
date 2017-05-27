package lila.i18n

import java.io._
import scala.concurrent.Future

import play.api.i18n.Lang
import play.api.libs.json.{ JsString, JsObject }

private[i18n] final class JsDump(path: String) {

  def keysToObject(keys: Seq[I18nKey], lang: Lang) = JsObject {
    keys.map { k =>
      k.key -> JsString(k.literalTo(lang, Nil))
    }
  }

  def keysToMessageObject(keys: Seq[I18nKey], lang: Lang) = JsObject {
    keys.map { k =>
      k.literalEn() -> JsString(k.literalTo(lang, Nil))
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
      """"%s":"%s"""".format(key, escape(Translator.literal(key, Nil, lang)))
    }.mkString("{", ",", "}")

  private def writeRefs = writeFile(
    new File("%s/refs.json".format(pathFile.getCanonicalPath)),
    LangList.all.toList.sortBy(_._1.code).map {
      case (lang, name) => s"""["${lang.code}","$name"]"""
    }.mkString("[", ",", "]")
  )

  private def writeFullJson = I18nDb.langs foreach { lang =>
    val code = dumpFromKey(I18nDb.all(defaultLang).keys, lang)
    val file = new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.language))
    writeFile(new File("%s/%s.all.json".format(pathFile.getCanonicalPath, lang.language)), code)
  }

  private def writeFile(file: File, content: String) = {
    val out = new PrintWriter(file)
    try { out.print(content) }
    finally { out.close }
  }

  private def escape(text: String) = text.replace(""""""", """\"""")
}
