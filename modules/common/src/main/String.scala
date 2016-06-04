package lila.common

import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement

object String {

  private val slugR = """[^\w-]""".r

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  final class Delocalizer(netDomain: String) {

    private val regex = ("""\w{2}\.""" + quoteReplacement(netDomain)).r

    def apply(url: String) = regex.replaceAllIn(url, netDomain)
  }

  def shorten(text: String, length: Int, sep: String = "…") = {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) (t take length) ++ sep
    else t
  }
}
