package lila.i18n

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, InputStream }
import java.util.zip.{ ZipEntry, ZipFile }
import play.api.i18n.Lang
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.jdk.CollectionConverters.*
import scala.xml.XML
import scala.concurrent.duration.*

object Registry:
  private val workers = 1 // TODO add to config

  val all: Messages =
    lila.common.Chronometer.syncEffect(loadSerialized): lap =>
      logger.info(s"Loaded ${lap.result.size} langs in ${lap.showDuration}")

  private def loadSerialized: Messages =

    val zipFile = new ZipFile(new java.io.File(getClass.getClassLoader.getResource("I18n.zip").toURI))

    val batches = zipFile
      .entries()
      .asScala
      .collect:
        case entry if !entry.isDirectory && entry.getSize > 0 => (entry, zipFile.getInputStream(entry))
      .toMap
      .grouped(workers)
      .map: batch =>
        Future
          .traverse(batch) { case (entry, inputStream) =>
            Future {
              val Array(db, langCode) = entry.getName.split("/")
              Lang(langCode) -> parseXml(inputStream, db)
            }
              .recover { case e: Throwable =>
                logger.error(s"Failed to parse ${entry.getName}", e)
                Lang("en-US") -> Map.empty[MessageKey, Translation]
              }
          }
          .map(_.toMap)
      .foldLeft(fuccess(Map.empty[Lang, MessageMap])) { (acc, batchResult) =>
        for
          accMap   <- acc
          batchMap <- batchResult
        yield accMap ++ batchMap.map { case (lang, messageMap) =>
          lang -> (accMap.getOrElse(lang, Map.empty) ++ messageMap)
        }
      }
      .recover { case e: Throwable =>
        logger.error(s"Failed to process batch", e)
        Map.empty[Lang, MessageMap]
      }

    val result = Await.result(batches, Duration.Inf)
    zipFile.close()
    result

  val default: Map[MessageKey, Translation] = all.getOrElse(defaultLang, Map.empty)

  val langs: Set[Lang] = all.keySet

  private def parseXml(xmlStream: InputStream, db: String): Map[MessageKey, Translation] =
    val xml = XML.load(xmlStream)
    xml.child
      .filterNot(node => node.isInstanceOf[scala.xml.Text] && node.text.trim.isEmpty)
      .map { e =>
        // new scala.xml.PrettyPrinter(80, 2).format(e).pp
        val key = s"$db:${e.\("@name").toString}"
        val value = e.label match
          case "string" => singleOrEscaped(e.text)
          case "plurals" =>
            val plurals = e.child
              .filter(_.label == "item")
              .map { i =>
                I18nQuantity.fromString(i.\("@quantity").toString).get -> i.text
              }
              .toMap
            Plurals(plurals)
        key -> value
      }
      .toMap

  private def singleOrEscaped(s: String) =
    val sb = new java.lang.StringBuilder(s.length + 10) // wet finger style
    var i     = 0 // i'm not sure what wet finger style is and i do not want to know
    var dirty = false
    while i < s.length do
      s.charAt(i) match
        case '<'  => sb.append("&lt;"); dirty = true
        case '>'  => sb.append("&gt;"); dirty = true
        case '&'  => sb.append("&amp;"); dirty = true
        case '"'  => sb.append("&quot;"); dirty = true
        case '\'' => sb.append("&#39;"); dirty = true
        case '\r' => dirty = true
        case '\n' => sb.append("<br>"); dirty = true
        case c    => sb.append(c)
      i += 1
    if dirty then Escaped(s, sb.toString.replace("\\&quot;", "&quot;"))
    else Simple(s)
