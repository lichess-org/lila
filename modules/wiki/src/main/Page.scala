package lila.wiki

import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement

case class Page(
  id: String,
  slug: String,
  number: Int,
  lang: String,
  title: String,
  body: String) {

  def isDefaultLang = lang == Page.DefaultLang
}

object Page {

  val DefaultLang = "en"
  val NameRegex = """^(\w{2,3})_(\d+)_(.+)$""".r

  // name = en_1_Some Title
  def make(name: String, body: String): Option[Page] = name match {
    case NameRegex(lang, numberStr, title) =>
      parseIntOption(numberStr) map { number =>
        Page(
          id = name,
          number = number,
          slug = slugify(title),
          lang = lang,
          title = title.replace("-", " "),
          body = body)
      }
    case _ => none
  }

  // does not lowercase
  private def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "_")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    """[^\w-]""".r.replaceAllIn(normalized, "")
  }

  private def dropNumber(input: String) =
    """^\d+_(.+)$""".r.replaceAllIn(input, m => quoteReplacement(m group 1))

  import lila.db.dsl.BSONJodaDateTimeHandler
  implicit val PageBSONHandler = reactivemongo.bson.Macros.handler[Page]
}
