import _root_.java.io.File
import _root_.java.nio.file.{Files, StandardCopyOption}
import sbt._, Keys._

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
        val pairs = readLines(file) flatMap makePair
        printToFile(compileToFile) {
          render(locale, pairs)
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
object Registry {

  def load = Map[Lang, Map[MessageKey, Translation]]($content)
}
"""
    }
    file
  }

  private def render(locale: String, pairs: List[(String, String)]) = {
    def quote(msg: String) = s"""""\"$msg""\""""
    // val content = pairs.map {
    //   case (key, message) => s""""$key"->${quote(message)}"""
    // } mkString ",\n"
    val content = ""
    s"""package lila.i18n
package db

// format: OFF
private object `$locale` {

  def load = Map[MessageKey, Translation]($content)
}
"""
  }

  private def makePair(str: String): Option[(String, String)] = for {
    fields <- CSVParser.parse(str, ',', '"')
    key <- fields.headOption
    translation <- fields.lift(3)
  } yield (key, translation)

  private def readLines(f: File) =
    scala.io.Source.fromFile(f)("UTF-8").getLines.toList

  private def printToFile(f: File)(content: String): Unit = {
    val p = new java.io.PrintWriter(f, "UTF-8")
    try { content.foreach(p.print) } finally { p.close() }
  }
}
