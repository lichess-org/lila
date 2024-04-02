package lila.core

import java.text.Normalizer

object slug:
  private val slugR              = """[^\w-]""".r
  private val slugMultiDashRegex = """-{2,}""".r

  def apply(input: String) =
    val nowhitespace = input.trim.replace(' ', '-')
    val singleDashes = slugMultiDashRegex.replaceAllIn(nowhitespace, "-")
    val normalized   = Normalizer.normalize(singleDashes, Normalizer.Form.NFD)
    val slug         = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase
