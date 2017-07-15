import _root_.java.io.File
import sbt._, Keys._
import scala.xml.XML

object MessageCompiler {

  def apply(sourceDir: File, destDir: File, dbs: List[String], compileTo: File): Seq[File] =
    dbs.flatMap { db =>
      doFile(
        db = db,
        sourceFile = sourceDir / s"$db.xml",
        destDir = destDir / db,
        compileTo = compileTo / db)
    }

  private def doFile(db: String, sourceFile: File, destDir: File, compileTo: File): Seq[File] = {
    val startsAt = System.currentTimeMillis()
    destDir.mkdirs()
    val registry = ("en-GB" -> sourceFile) :: destDir.list.toList.map { f =>
      f.takeWhile('.' !=) -> (destDir / f)
    }.sortBy(_._1)
    compileTo.mkdirs()
    val registryFile = writeRegistry(db, compileTo, registry)
    val res = for (entry <- registry) yield {
      val (locale, file) = entry
      val compileToFile = compileTo / s"$locale.scala"
      if (file.lastModified > compileToFile.lastModified) {
        printToFile(compileToFile) {
          render(db, locale, file)
        }
      }
      compileToFile
    }
    println(s"MessageCompiler took ${System.currentTimeMillis() - startsAt}ms")
    registryFile :: res
  }

  private def writeRegistry(db: String, compileTo: File, registry: List[(String, File)]) = {
    val file = compileTo / "Registry.scala"
    printToFile(file) {
      val content = registry.map {
        case (locale, _) => s"""Lang("${locale.replace("-", "\",\"")}")->`$locale`.load"""
      } mkString ",\n"
      s"""package lila.i18n
package db.$db

import play.api.i18n.Lang

// format: OFF
private[i18n] object Registry {

  def load = Map[Lang, Map[MessageKey, Translation]]($content)
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

  private def render(db: String, locale: String, file: File) = {
    val xml = XML.loadFile(file)
    def quote(msg: String) = s"""""\"$msg""\""""
    val content = xml.child.collect {
      case e if e.label == "string" =>
        val safe = escape(e.text)
        val escaped = escapeHtmlOption(safe).fold("None")(e => s"""Some(\"\"\"$e\"\"\")""")
        s"""(${toKey(e)},new Literal(\"\"\"$safe\"\"\",$escaped))"""
      case e if e.label == "plurals" =>
        val items = e.child.filter(_.label == "item").map { i =>
          s"""${ucfirst(i.\("@quantity").toString)}->\"\"\"${escape(i.text)}\"\"\""""
        }
        s"""(${toKey(e)},new Plurals(Map(${items mkString ","})))"""
    }
    s"""package lila.i18n
package db.$db

import I18nQuantity._

// format: OFF
private object `$locale` {

  def load = Map[MessageKey, Translation](\n${content mkString ",\n"})
}
"""
  }

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
