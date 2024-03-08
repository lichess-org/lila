import java.io.{ File, FileOutputStream, ObjectOutputStream }
import java.util.{ HashMap, ArrayList }
import sbt._
import scala.jdk.CollectionConverters.*

object I18n {
  def serialize(
      sourceDir: File,
      destDir: File,
      dbs: List[String],
      outputFile: File
  ): Seq[File] = {
    val locales = new ArrayList[String]()
    locales.add("en-GB")
    locales.addAll((destDir / "site").listFiles.map(_.getName.takeWhile(_ != '.')).sorted.toList.asJava)

    val translationMap = new HashMap[String, java.util.Map[String, Object]]()
    locales.forEach { locale =>
      translationMap.put(locale, makeMap(locale, sourceDir, destDir, dbs.asJava))
    }

    outputFile.getParentFile.mkdirs()
    val out = new ObjectOutputStream(new FileOutputStream(outputFile))
    out.writeObject(translationMap)
    out.close()

    Seq(outputFile)
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
            case _ => // how does one log something in a resource generator?
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
