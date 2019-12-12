package views.html.plan

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object badCheckout {

  def apply(err: String)(implicit ctx: Context) =
    views.html.site.message("Payment can not be processed.")(
      p("The payment didn't go through. Your card was not charged."),
      p(err)
    )
}
