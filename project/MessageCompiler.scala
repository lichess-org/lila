import _root_.java.io.File
import _root_.java.nio.file.{ Files, StandardCopyOption }
import sbt._, Keys._

object MessageCompiler {

  def apply(src: File, dst: File): Seq[File] = {
    val startsAt = System.currentTimeMillis()
    val sourceFiles = Option(src.list) getOrElse Array() filter (_ startsWith "messages")
    val registry = sourceFiles.toList.map { f =>
      f.split('.') match {
        case Array("messages", lang) => lang -> f
        case Array("messages")       => "default" -> f
      }
    }
    dst.mkdirs()
    val registryFile = writeRegistry(dst, registry)
    val res = for (entry <- registry) yield {
      val (lang, file) = entry
      val srcFile = src / file
      val dstFile = dst / s"$lang.scala"
      if (srcFile.lastModified > dstFile.lastModified) {
        val pairs = readLines(srcFile) map makePair
        printToFile(dstFile) {
          render(lang, pairs)
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
        case (lang, _) => s""""$lang"->$lang.load"""
      } mkString ",\n"
      s"""package lila.i18n
package db

object Registry {

  def load = Map[String, Map[String, String]]($content)
}
"""
    }
    file
  }

  private def render(lang: String, pairs: List[(String, String)]) = {
    def quote(msg: String) = s"""""\"$msg""\""""
    val content = pairs.map {
      case (key, message) => s""""$key"->${quote(message)}"""
    } mkString ",\n"
    s"""package lila.i18n
package db

private object $lang {

  def load = Map[String, String]($content)
}
"""
  }

  private def makePair(str: String): (String, String) = {
    val p = str.splitAt(str indexOf "=")
    p._1 -> p._2.drop(1)
  }

  private def readLines(f: File) =
    scala.io.Source.fromFile(f).getLines.toList

  private def printToFile(f: File)(content: String): Unit = {
    val p = new java.io.PrintWriter(f)
    try { content.foreach(p.print) } finally { p.close() }
  }
}
