package lila
package wiki

import user.User

import org.joda.time.DateTime
import com.novus.salat.annotations.Key
import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement

case class Page(
    @Key("_id") id: String,
    name: String,
    title: String,
    body: String) {

  def slug = id
}

object Page {

  def apply(name: String, body: String): Page = new Page(
    id = dropNumber(slugify(name)),
    name = name,
    title = dropNumber(name.replace("-", " ")),
    body = body)

  private def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "_")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    """[^\w-]""".r.replaceAllIn(normalized, "")
  }

  private def dropNumber(input: String) = 
    """^\d+_(.+)$""".r.replaceAllIn(input, m => quoteReplacement(m group 1))
}
