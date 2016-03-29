import _root_.java.io.File
import _root_.java.nio.file.Files
import sbt._, Keys._

object MessageCompiler {

  def apply(src: File, dst: File): Seq[File] = {
    println(src.toPath)
    println(dst.toPath)
    val sourceFiles = Option(src.list) getOrElse Array() filter (_ startsWith "messages")
    if (sourceFiles.nonEmpty) dst.mkdirs()
    for (file <- sourceFiles) yield {
      val srcFile = src / file
      val dstFile = dst / ((file take (file lastIndexOf '.')) + ".scala")
      println(srcFile.toPath)
      println(dstFile.toPath)
      Files.copy(srcFile.toPath, dstFile.toPath)
      dstFile
    }
  }
}
