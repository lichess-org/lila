package lila.wiki

case class Page(id: String, name: String, title: String, body: String) {

  def slug = id
}

object Page {

  import java.text.Normalizer
  import java.util.regex.Matcher.quoteReplacement

  def make(name: String, body: String): Page = new Page(
    id = dropNumber(slugify(name)),
    name = name,
    title = dropNumber(name.replace("-", " ")),
    body = body)

  import lila.db.Tube
  import play.api.libs.json._

  val tube = Tube(Json.reads[Page], Json.writes[Page]) 

  // does not lowercase
  private def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "_")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    """[^\w-]""".r.replaceAllIn(normalized, "")
  }

  private def dropNumber(input: String) =
    """^\d+_(.+)$""".r.replaceAllIn(input, m ⇒ quoteReplacement(m group 1))
}

