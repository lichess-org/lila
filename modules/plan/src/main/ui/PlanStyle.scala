package lila.plan
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class PlanStyle(helpers: Helpers):
  import helpers.{ *, given }

  def selector(me: Me)(using Context) =
    div(cls := "patron-style-selector")("Wing selector")
