package lila.i18n

import java.io.{ File, FileInputStream, ObjectInputStream }
import java.util.{ Map as JMap }
import play.api.i18n.Lang
import scala.jdk.CollectionConverters.*

object Registry:

  val all: Map[Lang, MessageMap] = lila.common.Chronometer.syncEffect(loadSerialized): lap =>
    logger.info(s"Loaded ${lap.result.size} langs in ${lap.showDuration}")

  private def loadSerialized: Map[Lang, MessageMap] =
    val istream = new ObjectInputStream(getClass.getClassLoader.getResourceAsStream("I18n.ser"))
    val javaMap = istream.readObject().asInstanceOf[JMap[String, JMap[String, Object]]].asScala
    istream.close()

    javaMap.toMap.map:
      case (langCode, messageMap) =>
        Lang(langCode) -> messageMap.asScala
          .map:
            case (key, value) =>
              key -> (value match
                case s: String => singleOrEscaped(s)
                case m: JMap[?, ?] =>
                  val plurals = m
                    .asInstanceOf[JMap[String, String]]
                    .asScala
                    .flatMap:
                      case (q, i: String) =>
                        I18nQuantity
                          .fromString(q)
                          .map: quantity =>
                            quantity -> i
                  Plurals(plurals.toMap)
                case _ => throw new Exception(s"i18n oh noes $key: $value")
              )
          .asJava

  val default: MessageMap = all.getOrElse(defaultLang, new java.util.HashMap[MessageKey, Translation])

  val langs: Set[Lang] = all.keySet

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
