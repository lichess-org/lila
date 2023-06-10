package lila.app
package templating

import lila.api.WebContext
import lila.app.ui.ScalatagsTemplate.*

trait FlashHelper { self: I18nHelper =>

  def standardFlash(using WebContext): Option[Tag] =
    successFlash orElse warningFlash orElse failureFlash

  def successFlash(using ctx: WebContext): Option[Tag] =
    ctx.flash("success").map { msg =>
      flashMessage("success")(
        if msg.isEmpty then trans.success()(using ctx.lang) else msg
      )
    }

  def warningFlash(using ctx: WebContext): Option[Tag] =
    ctx.flash("warning").map { msg =>
      flashMessage("warning")(
        if (msg.isEmpty) "Warning" else msg
      )
    }

  def failureFlash(using ctx: WebContext): Option[Tag] =
    ctx.flash("failure").map { msg =>
      flashMessage("failure")(
        if (msg.isEmpty) "Failure" else msg
      )
    }

  def flashMessage(color: String)(content: Modifier*): Tag =
    flashMessageWith(cls := s"flash flash-$color")(content)

  def flashMessageWith(modifiers: Modifier*)(content: Modifier*): Tag =
    div(modifiers)(div(cls := "flash__content")(content))
}
