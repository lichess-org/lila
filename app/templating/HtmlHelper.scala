package lila.app
package templating

import play.twirl.api.Html
import ornicar.scalalib.Zero

trait HtmlHelper {

  val emptyHtml = Html("")
  implicit val LilaHtmlZero: Zero[Html] = Zero.instance(emptyHtml)
  implicit val LilaHtmlMonoid = scalaz.Monoid.instance[Html](
    (a, b) => Html(a.body + b.body),
    LilaHtmlZero.zero
  )

  val spinner = Html("""<div class="spinner"><svg viewBox="0 0 40 40"><circle cx=20 cy=20 r=18 fill="none"></circle></svg></div>""")
}
