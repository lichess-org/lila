package lila
package templating

import scala.math._
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Date
//import net.sf.jfuzzydate.{FuzzyDateFormat, FuzzyDateFormatter}

object StringHelper extends StringHelper

trait StringHelper {

  def slugify(input: String) = {
    val nowhitespace = input.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  //def formatDate(date: Time) = date format "dd MMMM yyy"

  //def formatDistance(date: Date) = fuzzyFormatter formatDistance date

  //private val fuzzyFormatter = FuzzyDateFormat.getInstance

  def pluralize(s: String, n: Int) = "%d %s%s".format(n, s, if (n > 1) "s" else "")
}
