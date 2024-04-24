package lila.mod
package ui

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.*
import lila.report.{ Report, Reason }

final class ModUi(helpers: KitchenSink):
  import helpers.{ *, given }

  def gdprEraseButton(u: User)(using Context) =
    val allowed = u.marks.clean || Granter.opt(_.Admin)
    submitButton(
      cls := (!allowed).option("disabled"),
      title := {
        if allowed
        then "Definitely erase everything about this user"
        else "This user has some history, only admins can erase"
      },
      (!allowed).option(disabled)
    )("GDPR erasure")
