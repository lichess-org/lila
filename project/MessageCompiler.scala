import _root_.java.io.File
import sbt._, Keys._
import scala.io.Source
import scala.xml.XML

object MessageCompiler {

  def apply(sourceDir: File, destDir: File, dbs: List[String], compileTo: File): Seq[File] =
    dbs.flatMap { db =>
      doFile(
        db = db,
        sourceFile = sourceDir / s"$db.xml",
        destDir = destDir / db,
        compileTo = compileTo / db
      )
    }

  private def doFile(db: String, sourceFile: File, destDir: File, compileTo: File): Seq[File] = {
    destDir.mkdirs()
    val registry = ("en-GB" -> sourceFile) :: destDir.list.toList.map { f =>
      f.takeWhile('.' !=) -> (destDir / f)
    }.sortBy(_._1)
    compileTo.mkdirs()
    var translatedLocales = Set.empty[String]
    val res = for {
      entry <- registry
      compilable <- {
        val (locale, file) = entry
        val compileToFile = compileTo / s"$locale.scala"
        if (!isFileEmpty(file)) {
          translatedLocales = translatedLocales + locale
          if (file.lastModified > compileToFile.lastModified) {
            printToFile(compileToFile)(render(db, locale, file))
          }
          Some(compileToFile)
        }
        else None
      }
    } yield compilable
    writeRegistry(db, compileTo, translatedLocales) :: res
  }

  private def isFileEmpty(f: File) = {
    Source.fromFile(f, "UTF-8").getLines.drop(2).next == "<resources></resources>"
  }

  private def writeRegistry(db: String, compileTo: File, locales: Iterable[String]) = {
    val file = compileTo / "Registry.scala"
    printToFile(file) {
      val content = locales.map { locale =>
        s"""Lang("${locale.replace("-", "\",\"")}")->`$locale`.load"""
      } mkString ",\n"
      s"""package lila.i18n
package db.$db

import play.api.i18n.Lang

// format: OFF
private[i18n] object Registry {

  def load = Map[Lang, java.util.HashMap[MessageKey, Translation]]($content)
}
"""
    }
    file
  }

  private def ucfirst(str: String) = str(0).toUpper + str.drop(1)

  private def toKey(e: scala.xml.Node) = s""""${e.\("@name")}""""

  private def escape(str: String) = {
    // is someone trying to inject scala code?
    if (str contains "\"\"\"") sys error s"Skipped translation: $str"
    // crowdin escapes ' and " with \, and encodes &. We'll do it at runtime instead.
    else str.replace("\\'", "'").replace("\\\"", "\"")
  }

  private def render(db: String, locale: String, file: File): String = {
    val xml = try {
      XML.loadFile(file)
    }
    catch {
      case e: Exception => println(file); throw e;
    }
    def quote(msg: String) = s"""""\"$msg""\""""
    val content = xml.child.collect {
      case e if e.label == "string" =>
        val safe = escape(e.text)
        val translation = escapeHtmlOption(safe) match {
          case None => s"""new Simple(\"\"\"$safe\"\"\")"""
          case Some(escaped) => s"""new Escaped(\"\"\"$safe\"\"\",\"\"\"$escaped\"\"\")"""
        }
        s"""m.put(${toKey(e)},$translation)"""
      case e if e.label == "plurals" =>
        val items: Map[String, String] = e.child.filter(_.label == "item").map { i =>
          ucfirst(i.\("@quantity").toString) -> s"""\"\"\"${escape(i.text)}\"\"\""""
        }.toMap
        s"""m.put(${toKey(e)},new Plurals(${pluralMap(items)}))"""
    }
    s"""package lila.i18n
package db.$db

import I18nQuantity._

// format: OFF
private object `$locale` {

  def load: java.util.HashMap[MessageKey, Translation] = {
    val m = new java.util.HashMap[MessageKey, Translation](${content.size + 1}, 1f)
${content mkString "\n"}
    m
  }
}
"""
  }

  private def pluralMap(items: Map[String, String]): String =
    if (items.size > 4) s"""Map(${items.map { case (k, v) => s"$k->$v" } mkString ","})"""
    else s"""new Map.Map${items.size}(${items.map { case (k, v) => s"$k,$v" } mkString ","})"""

  private def nl2br(html: String) =
    html.replace("\r\n", "<br />").replace("\n", "<br />")

  private val badChars = "[<>&\"']".r.pattern
  private def escapeHtmlOption(s: String): Option[String] = {
    if (badChars.matcher(s).find) Some {
      val sb = new StringBuilder(s.size + 10) // wet finger style
      var i = 0
      while (i < s.length) {
        sb.append {
          s.charAt(i) match {
            case '<' => "&lt;";
            case '>' => "&gt;";
            case '&' => "&amp;";
            case '"' => "&quot;";
            case '\'' => "&#39;";
            case c => c
          }
        }
        i += 1
      }
      nl2br(sb.toString)
    }
    else {
      val withBrs = nl2br(s)
      if (withBrs != s) Some(withBrs) else None
    }
  }

  private def printToFile(f: File)(content: String): Unit = {
    val p = new java.io.PrintWriter(f, "UTF-8")
    try { content.foreach(p.print) } finally { p.close() }
  }
}
