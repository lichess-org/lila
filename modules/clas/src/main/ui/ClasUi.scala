package lila.clas
package ui

import lila.ui.ScalatagsTemplate.{ *, given }

final class ClasUi(helpers: lila.ui.Helpers):
  import helpers.{ *, given }

  def showArchived(archived: Clas.Recorded)(using Translate) =
    div(
      trans.clas.removedByX(userIdLink(archived.by.some)),
      " ",
      momentFromNowOnce(archived.at)
    )
