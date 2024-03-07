package lila.i18n

import java.io.{ File, FileInputStream, ObjectInputStream }
import java.util.{ Map as JMap }
import play.api.i18n.Lang
import scala.jdk.CollectionConverters.*

object Registry:
  private val badChars = """[<>&"'\r\n]""".r.pattern

  val all: Map[Lang, MessageMap] =
    val istream = new ObjectInputStream(getClass.getClassLoader.getResourceAsStream("I18n.ser"))
    val javaMap = istream.readObject().asInstanceOf[JMap[String, JMap[String, Object]]].asScala
    istream.close()

    javaMap.toMap.map:
      case (langCode, messageMap) =>
        Lang(langCode) -> messageMap.asScala
          .map:
            case (key, value) =>
              key -> (value match
                case s: String => if badChars.matcher(s).find() then Escaped(s, escapeHtml(s)) else Simple(s)
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

  private def escapeHtml(s: String) =
    val sb = new java.lang.StringBuilder(s.length + 10) // wet finger style
    var i = 0 // i'm not sure what wet finger style is and i do not want to know
    while i < s.length do
      s.charAt(i) match
        case '<'  => sb.append("&lt;")
        case '>'  => sb.append("&gt;")
        case '&'  => sb.append("&amp;")
        case '"'  => sb.append("&quot;")
        case '\'' => sb.append("&#39;")
        case '\r' => ()
        case '\n' => sb.append("<br>")
        case c    => sb.append(c)
      i += 1
    sb.toString.replace("\\&quot;", "&quot;")
