package lila.app
package templating

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._

trait FlashHelper { self: I18nHelper =>

  def standardFlash(implicit ctx: Context): Option[Frag] =
    successFlash orElse failureFlash

  def successFlash(implicit ctx: Context): Option[Frag] =
    ctx.flash("success").map { msg =>
      div(cls := "flash flash-success")(
        if (msg.isEmpty) trans.success()
        else msg
      )
    }

  def failureFlash(implicit ctx: Context): Option[Frag] =
    ctx.flash("failure").map { msg =>
      div(cls := "flash flash-failure")(
        if (msg.isEmpty) "Failure"
        else msg
      )
    }
}
