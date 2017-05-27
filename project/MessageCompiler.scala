import _root_.java.io.File
import _root_.java.nio.file.{Files, StandardCopyOption}
import sbt._, Keys._
import scala.xml.XML

object MessageCompiler {

  def apply(sourceFile: File, destDir: File, compileTo: File): Seq[File] = {
    val startsAt = System.currentTimeMillis()
    val registry = ("en-GB" -> sourceFile) :: destDir.list.toList.map { f =>
      f.takeWhile('.' !=) -> (destDir / f)
    }.sortBy(_._1)
    compileTo.mkdirs()
    val registryFile = writeRegistry(compileTo, registry)
    val res = for (entry <- registry) yield {
      val (locale, file) = entry
      val compileToFile = compileTo / s"$locale.scala"
      if (file.lastModified > compileToFile.lastModified) {
        printToFile(compileToFile) {
          render(locale, file)
        }
      }
      compileToFile
    }
    println(s"MessageCompiler took ${System.currentTimeMillis() - startsAt}ms")
    registryFile :: res
  }

  private def writeRegistry(compileTo: File, registry: List[(String, File)]) = {
    val file = compileTo / "Registry.scala"
    printToFile(file) {
      val content = registry.map {
        case (locale, _) => s"""Lang("${locale.replace("-", "\",\"")}")->`$locale`.load"""
      } mkString ",\n"
      s"""package lila.i18n
package db

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

  private def render(locale: String, file: File) = {
    val xml = XML.loadFile(file)
    def quote(msg: String) = s"""""\"$msg""\""""
    val content = xml.child.collect {
      case e if e.label == "string" => s"""${toKey(e)}->Literal(\"\"\"${e.text}\"\"\")"""
      case e if e.label == "plurals" =>
        val items = e.child.filter(_.label == "item").map { i =>
          s"""${ucfirst(i.\("@quantity").toString)}->\"\"\"${i.text}\"\"\""""
        }
        s"""${toKey(e)}->Plurals(Map(${items mkString ","}))"""
    }
    s"""package lila.i18n
package db

import I18nQuantity._

// format: OFF
private object `$locale` {

  def load = Map[MessageKey, Translation](\n${content mkString ",\n"})
}
"""
  }

  private def printToFile(f: File)(content: String): Unit = {
    val p = new java.io.PrintWriter(f, "UTF-8")
    try { content.foreach(p.print) } finally { p.close() }
  }
}
