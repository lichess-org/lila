package lila.app
package templating

import lila.api.Context
import lila.app.ui.ScalatagsTemplate.{ *, given }

trait FlashHelper { self: I18nHelper =>

  def standardFlash(modifiers: Modifier*)(using ctx: Context): Option[Frag] =
    successFlash(modifiers) orElse warningFlash(modifiers) orElse failureFlash(modifiers)

  def successFlash(modifiers: Seq[Modifier])(using ctx: Context): Option[Frag] =
    ctx.flash("success").map { msg =>
      flashMessage(modifiers ++ Seq(cls := "flash-success"))(
        if (msg.isEmpty) trans.success()(using ctx.lang) else msg
      )
    }

  def warningFlash(modifiers: Seq[Modifier])(using ctx: Context): Option[Frag] =
    ctx.flash("warning").map { msg =>
      flashMessage(modifiers ++ Seq(cls := "flash-warning"))(
        if (msg.isEmpty) "Warning" else msg
      )
    }

  def failureFlash(modifiers: Seq[Modifier])(using ctx: Context): Option[Frag] =
    ctx.flash("failure").map { msg =>
      flashMessage(modifiers ++ Seq(cls := "flash-failure"))(
        if (msg.isEmpty) "Failure" else msg
      )
    }

  def flashMessage(modifiers: Seq[Modifier])(msg: Frag): Frag =
    flashMessage(modifiers*)(msg)

  def flashMessage(modifiers: Modifier*)(contentModifiers: Modifier*): Frag =
    div(modifiers)(cls := "flash")(
      div(cls := "flash__content")(contentModifiers)
    )
}
