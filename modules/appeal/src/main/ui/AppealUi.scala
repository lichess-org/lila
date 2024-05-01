package lila.appeal
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class AppealUi(helpers: Helpers):
  import helpers.{ *, given }

  def page(title: String)(using Context) =
    Page(title)
      .cssTag("form3")
      .cssTag("appeal")
      .cssTag(Granter.opt(_.UserModView).option("mod.user"))
      .js(EsmInit("bits.appeal") ++ Granter.opt(_.UserModView).so(EsmInit("mod.user")))
