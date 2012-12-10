package lila.core

object Text {

  def slugify(input: String) = {
    import java.text.Normalizer
    val nowhitespace = input.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = """[^\w-]""".r.replaceAllIn(normalized, "")
    slug.toLowerCase
  }
}
