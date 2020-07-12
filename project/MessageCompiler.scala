import _root_.java.io.File
import sbt._, Keys._
import scala.io.Source
import scala.xml.XML

object MessageCompiler {

  def apply(sourceDir: File, destDir: File, dbs: List[String], compileTo: File): Seq[File] = {
    compileTo.mkdirs()
    val locales: List[String] = "en-GB" ::
      (destDir / "site").list.toList.map { _.takeWhile('.' !=) }.sorted
    val localeFiles = locales
      .map { locale =>
        locale -> writeLocale(locale, sourceDir, destDir, compileTo, dbs)
      }
      .filter(_._2.exists)
    writeRegistry(compileTo, localeFiles.map(_._1)) :: localeFiles.map(_._2)
  }

  private def writeLocale(
      locale: String,
      sourceDir: File,
      destDir: File,
      compileTo: File,
      dbs: List[String]
  ): File = {
    val scalaFile = compileTo / s"$locale.scala"
    val xmlFiles =
      if (locale == "en-GB") dbs.map { db =>
        db -> (sourceDir / s"$db.xml")
      }
      else
        dbs.map { db =>
          db -> (destDir / db / s"$locale.xml")
        }

    val isNew = xmlFiles.exists {
      case (_, file) => !isFileEmpty(file) && file.lastModified > scalaFile.lastModified
    }
    if (!isNew) scalaFile
    else
      printToFile(scalaFile) {
        val puts = xmlFiles flatMap {
          case (db, file) =>
            try {
              val xml = XML.loadFile(file)
              xml.child.collect {
                case e if e.label == "string" =>
                  val safe = escape(e.text)
                  val translation = escapeHtmlOption(safe) match {
                    case None          => s"""new Simple(\"\"\"$safe\"\"\")"""
                    case Some(escaped) => s"""new Escaped(\"\"\"$safe\"\"\",\"\"\"$escaped\"\"\")"""
                  }
                  s"""m.put(${toKey(e, db)},$translation)"""
                case e if e.label == "plurals" =>
                  val items: Map[String, String] = e.child
                    .filter(_.label == "item")
                    .map { i =>
                      ucfirst(i.\("@quantity").toString) -> s"""\"\"\"${escape(i.text)}\"\"\""""
                    }
                    .toMap
                  s"""m.put(${toKey(e, db)},new Plurals(${pluralMap(items)}))"""
              }
            } catch {
              case _: Exception => Nil
            }
        }

        s"""package lila.i18n

${if (puts.exists(_ contains "new Plurals(")) "import I18nQuantity._" else ""}

// format: OFF
private object `$locale` {

  def load: java.util.HashMap[MessageKey, Translation] = {
    val m = new java.util.HashMap[MessageKey, Translation](${puts.size + 1})
${puts mkString "\n"}
    m
  }
}
"""
      }
  }

  private def isFileEmpty(file: File) = {
    !file.exists() || Source.fromFile(file, "UTF-8").getLines.drop(1).next == "<resources></resources>"
  }

  private def packageName(db: String) = if (db == "class") "clas" else db

  private def writeRegistry(destDir: File, locales: Iterable[String]) =
    printToFile(destDir / "Registry.scala") {
      val content = locales.map { locale =>
        s"""Lang("${locale.replace("-", "\",\"")}")->`$locale`.load"""
      } mkString ",\n"
      s"""package lila.i18n

import play.api.i18n.Lang

// format: OFF
private object Registry {

  val all = Map[Lang, java.util.HashMap[MessageKey, Translation]](\n$content)

  val default: java.util.HashMap[MessageKey, Translation] = all(defaultLang)

  val langs: Set[Lang] = all.keys.toSet
}
"""
    }

  private def ucfirst(str: String) = str(0).toUpper + str.drop(1)

  private def toKey(e: scala.xml.Node, db: String) =
    if (db == "site") s""""${e.\("@name")}""""
    else s""""$db:${e.\("@name")}""""

  private def quote(msg: String) = s"""""\"$msg""\""""

  private def escape(str: String) = {
    // is someone trying to inject scala code?
    if (str contains "\"\"\"") sys error s"Skipped translation: $str"
    // crowdin escapes ' and " with \, and encodes &. We'll do it at runtime instead.
    else str.replace("\\'", "'").replace("\\\"", "\"")
  }

  private def pluralMap(items: Map[String, String]): String =
    if (items.size > 4) s"""Map(${items.map { case (k, v) => s"$k->$v" } mkString ","})"""
    else s"""new Map.Map${items.size}(${items.map { case (k, v) => s"$k,$v" } mkString ","})"""

  private val badChars = """[<>&"'\r\n]""".r.pattern
  private def escapeHtmlOption(s: String): Option[String] =
    if (badChars.matcher(s).find) Some {
      val sb = new java.lang.StringBuilder(s.size + 10) // wet finger style
      var i  = 0
      while (i < s.length) {
        s.charAt(i) match {
          case '<'  => sb append "&lt;"
          case '>'  => sb append "&gt;"
          case '&'  => sb append "&amp;"
          case '"'  => sb append "&quot;"
          case '\'' => sb append "&#39;"
          case '\r' => ()
          case '\n' => sb append "<br />"
          case c    => sb append c
        }
        i += 1
      }
      sb.toString
    }
    else None

  private def printToFile(file: File)(content: String): File = {
    val p = new java.io.PrintWriter(file, "UTF-8")
    try {
      p.write(content)
    } finally {
      p.close()
    }
    file
  }
}
