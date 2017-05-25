import _root_.java.io.File
import _root_.java.nio.file.{Files, StandardCopyOption}
import sbt._, Keys._

object MessageCompiler {

  def apply(src: File, dst: File): Seq[File] = {
    val startsAt = System.currentTimeMillis()
    val sourceFiles = Option(src.list) getOrElse Array() filter (_ endsWith ".csv")
    val registry = sourceFiles.toList.map { f =>
      f.takeWhile('.' !=) -> f
    }
    dst.mkdirs()
    val registryFile = writeRegistry(dst, registry)
    val res = for (entry <- registry) yield {
      val (locale, file) = entry
      val srcFile = src / file
      val dstFile = dst / s"$locale.scala"
      if (srcFile.lastModified > dstFile.lastModified) {
        val pairs = readLines(srcFile) flatMap makePair
        printToFile(dstFile) {
          render(locale, pairs)
        }
      }
      dstFile
    }
    println(s"MessageCompiler took ${System.currentTimeMillis() - startsAt}ms")
    registryFile :: res
  }

  private def writeRegistry(dst: File, registry: List[(String, String)]) = {
    val file = dst / "Registry.scala"
    printToFile(file) {
      val content = registry.map {
        case (locale, _) => s""""$locale"->`$locale`.load"""
      } mkString ",\n"
      s"""package lila.i18n
package db

// format: OFF
object Registry {

  def load = Map[String, Map[String, String]]($content)
}
"""
    }
    file
  }

  private def render(locale: String, pairs: List[(String, String)]) = {
    def quote(msg: String) = s"""""\"$msg""\""""
    val content = pairs.map {
      case (key, message) => s""""$key"->${quote(message)}"""
    } mkString ",\n"
    s"""package lila.i18n
package db

// format: OFF
private object `$locale` {

  def load = Map[String, String]($content)
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
