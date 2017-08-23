package lila.common

import java.text.Normalizer
import java.util.regex.Matcher.quoteReplacement
import play.twirl.api.Html

object String {

  private val slugR = """[^\w-]""".r

  def slugify(input: String) = {
    val nowhitespace = input.trim.replace(" ", "-")
    val normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
    val slug = slugR.replaceAllIn(normalized, "")
    slug.toLowerCase
  }

  def decodeUriPath(input: String): Option[String] = {
    try {
      play.utils.UriEncoding.decodePath(input, "UTF-8").some
    } catch {
      case e: play.utils.InvalidUriEncodingException => None
    }
  }

  def shorten(text: String, length: Int, sep: String = "…") = {
    val t = text.replace("\n", " ")
    if (t.size > (length + sep.size)) (t take length) ++ sep
    else t
  }

  object base64 {
    import java.util.Base64
    import java.nio.charset.StandardCharsets
    def encode(txt: String) =
      Base64.getEncoder.encodeToString(txt getBytes StandardCharsets.UTF_8)
    def decode(txt: String): Option[String] = try {
      Some(new String(Base64.getDecoder decode txt))
    } catch {
      case _: java.lang.IllegalArgumentException => none
    }
  }

  object html {

    // from https://github.com/android/platform_frameworks_base/blob/d59921149bb5948ffbcb9a9e832e9ac1538e05a0/core/java/android/text/TextUtils.java#L1361
    def escape(s: String): Html = Html(escapeUnsafe(s))

    private val badChars = "[<>&\"']".r.pattern

    def escapeUnsafe(s: String): String = {
      if (badChars.matcher(s).find) {
        val sb = new StringBuilder(s.size + 10) // wet finger style
        var i = 0
        while (i < s.length) {
          sb.append {
            s.charAt(i) match {
              case '<' => "&lt;"
              case '>' => "&gt;"
              case '&' => "&amp;"
              case '"' => "&quot;"
              case '\'' => "&#39;"
              case c => c
            }
          }
          i += 1
        }
        sb.toString
      } else s
    }
  }
}
