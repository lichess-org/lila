import _root_.java.io.File
import sbt._, Keys._
import scala.io.Source
import scala.xml.XML

object MessageCompiler {

  def apply(sourceDir: File, destDir: File, dbs: List[String], compileTo: File): Seq[File] = {
    compileTo.mkdirs()
    val locales: List[String] = "en-GB" ::
      (destDir / "site").list.toList.map { _.takeWhile('.' !=) }.sorted
    val localeFiles = locales
      .map { locale =>
        locale -> writeLocale(locale, sourceDir, destDir, compileTo, dbs)
      }
      .filter(_._2.exists)
    writeRegistry(compileTo, localeFiles.map(_._1)) :: localeFiles.map(_._2)
  }

  private def writeLocale(
      locale: String,
      sourceDir: File,
      destDir: File,
      compileTo: File,
      dbs: List[String]
  ): File = {
    val underLocale = locale.replace("-", "_")
    val javaFile    = compileTo / s"$underLocale.java"
    val xmlFiles =
      if (locale == "en-GB") dbs.map { db =>
        db -> (sourceDir / s"$db.xml")
      }
      else
        dbs.map { db =>
          db -> (destDir / db / s"$locale.xml")
        }

    val isNew = xmlFiles.exists { case (_, file) =>
      !isFileEmpty(file) && file.lastModified > javaFile.lastModified
    }
    if (!isNew) javaFile
    else
      printToFile(javaFile) {
        val puts = xmlFiles flatMap { case (db, file) =>
          try {
            val xml = XML.loadFile(file)
            xml.child.collect {
              case e if e.label == "string" =>
                val safe             = escape(e.text)
                val safeWithNewLines = escapeNewLines(safe)
                val translation = escapeHtmlOption(safe) match {
                  case None          => s"""new Simple(\"$safeWithNewLines\")"""
                  case Some(escaped) => s"""new Escaped(\"$safeWithNewLines\",\"$escaped\")"""
                }
                s"""m.put(${toKey(e, db)},$translation);"""
              case e if e.label == "plurals" =>
                val allItems: Map[String, String] = e.child
                  .filter(_.label == "item")
                  .map { i =>
                    ucfirst(i.\("@quantity").toString) -> s"""\"${escapeNewLines(i.text)}\""""
                  }
                  .toMap
                val otherValue = allItems.get("Other")
                val default    = allItems.head
                // The following optimisation aims to drop duplicated translations
                // to reduce the size of the generated Java code.
                val items = allItems.filter {
                  case pair if pair == default          => true
                  case ("Other", v)                     => v != default._2
                  case (_, v) if v == default._2        => false
                  case (_, v) if otherValue.contains(v) => false
                  case _                                => true
                }
                s"""m.put(${toKey(e, db)},new Plurals(${pluralMap(items)}));"""
            }
          } catch {
            case _: Exception => Nil
          }
        }

        val loadFactor      = 0.75
        val initialCapacity = (puts.size / loadFactor).toInt + 1
        val fullMapImports =
          if (puts.exists(_.contains("ScalaRunTime$")))
            """import scala.Predef$;
import scala.Tuple2;
import scala.Tuple2$;
import scala.runtime.ScalaRunTime$;"""
          else ""

        s"""package lila.i18n;
import java.util.HashMap;
import scala.collection.immutable.Map;
$fullMapImports
public final class $underLocale {
public static final HashMap<String, Translation> load() {
HashMap<String, Translation> m = new HashMap<String, Translation>($initialCapacity, ${loadFactor}f);
${puts mkString "\n"}
return m;
}
}
"""
      }
  }

  private def isFileEmpty(file: File) =
    !file.exists() || {
      val source = Source.fromFile(file, "UTF-8")
      try {
        source.getLines.drop(1).next == "<resources></resources>"
      } finally {
        source.close()
      }
    }

  private def packageName(db: String) = if (db == "class") "clas" else db

  private def writeRegistry(destDir: File, locales: Iterable[String]) =
    printToFile(destDir / "Registry.scala") {
      val content = locales.map { locale =>
        s"""Lang("${locale.replace("-", "\",\"")}")->${locale.replace("-", "_")}.load()"""
      } mkString ",\n"
      s"""package lila.i18n

import play.api.i18n.Lang

// format: OFF
private object Registry {

  val all = Map[Lang, MessageMap](\n$content)

  val default: MessageMap = all(defaultLang)

  val langs: Set[Lang] = all.keys.toSet
}
"""
    }

  private def ucfirst(str: String) = str(0).toUpper + str.drop(1)

  private def toKey(e: scala.xml.Node, db: String) =
    if (db == "site") s""""${e.\("@name")}""""
    else s""""$db:${e.\("@name")}""""

  private val doubleUserBackslashRegex = """(\\.)""".r

  private def doubleUserBackslash(str: String) =
    doubleUserBackslashRegex.replaceAllIn(
      str,
      m => (if (Set("""\"""", """\n""")(m.group(1))) "\\" else "\\\\\\") + m.group(1)
    )

  private def escape(str: String) =
    doubleUserBackslash(
      str
        .replace("\\\"", "\"") // remove \" escaping, which is not always present
        .replace("\\'", "'")
        .replace("\"", "\\\"") // escape " for sure
    )
  private def escapeNewLines(str: String) = str.replace("\n", "\\n")

  private def pluralMap(items: Map[String, String]): String =
    if (items.size > 4) {
      val mapItems = items.map { case (k, v) =>
        """Tuple2$.MODULE$.apply""" + s"""(I18nQuantity$$.$k, $v)"""
      } mkString ","
      """(Map)Predef$.MODULE$.Map().apply(ScalaRunTime$.MODULE$.wrapRefArray(new Tuple2[]{""" + mapItems + """}))"""
    } else
      s"""new Map.Map${items.size}<I18nQuantity, String>(${items.map { case (k, v) =>
          s"I18nQuantity$$.$k,$v"
        } mkString ","})"""

  private val badChars = """[<>&"'\r\n]""".r.pattern
  private def escapeHtmlOption(s: String): Option[String] =
    if (badChars.matcher(s).find) Some {
      val sb = new java.lang.StringBuilder(s.length + 10) // wet finger style
      var i  = 0
      while (i < s.length) {
        s.charAt(i) match {
          case '<'  => sb append "&lt;"
          case '>'  => sb append "&gt;"
          case '&'  => sb append "&amp;"
          case '"'  => sb append "&quot;"
          case '\'' => sb append "&#39;"
          case '\r' => ()
          case '\n' => sb append "<br>"
          case c    => sb append c
        }
        i += 1
      }
      sb.toString.replace("\\&quot;", "&quot;")
    }
    else None

  private def printToFile(file: File)(content: String): File = {
    val p = new java.io.PrintWriter(file, "UTF-8")
    try {
      p.write(content)
    } finally {
      p.close()
    }
    file
  }
}
