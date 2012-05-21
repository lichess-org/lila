package lila
package templating

import scala.math._
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date

object StringHelper extends StringHelper

trait StringHelper {

  def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def shorten(text: String, length: Int): String =
    text.replace("\n", " ") take length

  def pluralize(s: String, n: Int) = "%d %s%s".format(n, s, if (n > 1) "s" else "")

  //implicit def richString(str: String) = new {

    //def capitalize = str(0).toUpperCase + str.drop(1)
  //}

  def showNumber(n: Int): String = (n > 0).fold("+" + n, n.toString)
}
