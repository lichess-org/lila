import java.io.{ File, FileOutputStream, ObjectOutputStream }
import java.util.{ HashMap, ArrayList }
import sbt._
import scala.jdk.CollectionConverters.*

object I18n {
  def serialize(
      sourceDir: File,
      destDir: File,
      dbs: List[String],
      outputDir: File
  ): Seq[File] = {
    val locales = "en-GB" :: (destDir / "site").listFiles.map(_.getName.takeWhile(_ != '.')).sorted.toList

    outputDir.mkdirs()

    val files = locales.map { locale =>
      val file = new File(outputDir, s"i18n.$locale.ser")
      val translations = makeMap(locale, sourceDir, destDir, dbs.asJava)
      val out = new ObjectOutputStream(new FileOutputStream(file))
      out.writeObject(translations)
      out.close()
      file
    }
    files
  }

  private def makeMap(
      locale: String,
      sourceDir: File,
      destDir: File,
      dbs: java.util.List[String]
  ) = {
    val result = new HashMap[String, Object]()
    dbs.forEach { db =>
      val file =
        if (locale == "en-GB") new File(sourceDir, s"$db.xml")
        else new File(destDir, s"$db/$locale.xml")
      if (file.exists && file.isFile) {
        val xml = scala.xml.XML.loadFile(file)
        xml.child.foreach { e =>
          val key = toKey(e, db)
          e.label match {
            case "string" =>
              result.put(key, unescapeQuotes(e.text))
            case "plurals" =>
              val plurals = new HashMap[String, String]()
              e.child.filter(_.label == "item").foreach { i =>
                plurals.put(i.\("@quantity").toString, unescapeQuotes(i.text))
              }
              result.put(key, plurals)
            case _ =>
          }
        }
      }
    }
    result
  }

  private def unescapeQuotes(s: String) =
    s.replace("\\\"", "\"").replace("\\'", "'")

  private def toKey(e: scala.xml.Node, db: String) =
    if (db == "site") e.\("@name").toString
    else s"$db:${e.\("@name")}"
}
