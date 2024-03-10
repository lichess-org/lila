package lila.i18n

import java.io.{ File, FileInputStream, ObjectInputStream }
import java.util.{ Map as JMap }
import play.api.i18n.Lang
import scala.jdk.CollectionConverters.*
import lila.common.Chronometer

object Registry:

  val all: Map[Lang, MessageMap] = Chronometer.syncEffect(loadSerialized): lap =>
    logger.info(s"Loaded ${lap.result.size} langs in ${lap.showDuration}")

  private def loadSerialized: Map[Lang, MessageMap] =
    val istream = ObjectInputStream(getClass.getClassLoader.getResourceAsStream("I18n.ser"))
    val unserialized =
      Chronometer.syncEffect(
        istream.readObject().asInstanceOf[JMap[String, JMap[String, Object]]].asScala.toMap
      ): lap =>
        logger.info(s"Unserialized I18n.ser in ${lap.showDuration}")
    istream.close()

    unserialized.map:
      case (langCode, messageMap) =>
        Lang(langCode) -> messageMap.asScala
          .map: (key, value) =>
            key -> value.match
              case s: String => singleOrEscaped(s)
              case m: JMap[?, ?] =>
                val plurals = m
                  .asInstanceOf[JMap[String, String]]
                  .asScala
                  .flatMap: (q, i) =>
                    I18nQuantity
                      .fromString(q)
                      .map: quantity =>
                        quantity -> i
                Plurals(plurals.toMap)
              case _ => throw Exception(s"i18n oh noes $key: $value")
          .asJava

  val default: MessageMap = all.getOrElse(defaultLang, java.util.HashMap[MessageKey, Translation])

  private def singleOrEscaped(s: String) =
    val sb = java.lang.StringBuilder(s.length + 10)

    var dirty = false
    var i     = 0
    while i < s.length do
      s.charAt(i) match
        case '<' =>
          sb.append("&lt;")
          dirty = true
        case '>' =>
          sb.append("&gt;")
          dirty = true
        case '&' =>
          sb.append("&amp;")
          dirty = true
        case '"' =>
          sb.append("&quot;")
          dirty = true
        case '\'' =>
          sb.append("&#39;")
          dirty = true
        case '\n' =>
          sb.append("<br>")
          dirty = true
        case '\r' => dirty = true
        case c    => sb.append(c)
      i += 1
    if dirty then Escaped(s, sb.toString.replace("\\&quot;", "&quot;"))
    else Simple(s)
