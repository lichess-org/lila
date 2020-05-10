package views.html.plan

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object badCheckout {

  def apply(err: String)(implicit ctx: Context) =
    views.html.site.message("Payment can not be processed.")(
      p("The payment didn't go through. Your card was not charged."),
      p(err)
    )
}
