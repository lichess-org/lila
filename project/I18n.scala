import java.io.{ File, FileOutputStream, FileInputStream }
import java.util.zip.{ ZipEntry, ZipOutputStream }
import sbt._
import scala.xml.XML

object I18n {
  def serialize(sourceDir: File, destDir: File, dbs: List[String], outputFile: File): Seq[File] = {
    val zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile))

    def addToZip(file: File, name: String): Unit = {
      val entry = new ZipEntry(name)
      zipOutputStream.putNextEntry(entry)
      val in     = new FileInputStream(file)
      val buffer = new Array[Byte](1024)
      var len    = in.read(buffer)
      while (len > 0) {
        zipOutputStream.write(buffer, 0, len)
        len = in.read(buffer)
      }
      in.close()
      zipOutputStream.closeEntry()
    }

    dbs.foreach { db =>
      val enFile = new File(sourceDir, s"$db.xml")
      if (enFile.exists && enFile.isFile) addToZip(enFile, s"$db/en-GB")

      val everythingElseDir = new File(destDir, db)
      if (everythingElseDir.exists && everythingElseDir.isDirectory) {
        everythingElseDir.listFiles.filter(_.isFile).foreach { file =>
          val langCode = file.getName.takeWhile(_ != '.')
          addToZip(file, s"$db/$langCode")
        }
      }
    }

    zipOutputStream.close()
    Seq(outputFile)
  }
}
