package lila
package common

import java.text.Normalizer

object String {

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }
}
