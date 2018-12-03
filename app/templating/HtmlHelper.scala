package lidraughts.app
package templating

import ornicar.scalalib.Zero
import play.twirl.api.Html

trait HtmlHelper {

  val emptyHtml = Html("")

  implicit val LidraughtsHtmlZero: Zero[Html] = Zero.instance(emptyHtml)

  implicit val LidraughtsHtmlMonoid = scalaz.Monoid.instance[Html](
    (a, b) => Html(a.body + b.body),
    LidraughtsHtmlZero.zero
  )

  val spinner = Html("""<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>""")

  @inline implicit def toPimpedHtml(html: Html) = new PimpedHtml(html)
}

final class PimpedHtml(private val self: Html) extends AnyVal {
  def ++(other: Html): Html = Html(s"${self.body}${other.body}")
  def ++(other: String): Html = Html(s"${self.body}${other}")
}
