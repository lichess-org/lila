package lila.clas
package ui

import lila.ui.ScalatagsTemplate.{ *, given }
import lila.ui.*

final class ClasUi(helpers: KitchenSink):
  import helpers.{ *, given }

  def showArchived(archived: Clas.Recorded)(using Translate) =
    div(
      trans.clas.removedByX(userHelper.userIdLink(archived.by.some)),
      " ",
      momentFromNowOnce(archived.at)
    )
