package views
package html.puzzle

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.I18nKey
import lila.puzzle.{ Puzzle, PuzzleAngle, PuzzleOpening, PuzzleOpeningCollection, PuzzleTheme }
import lila.puzzle.PuzzleOpening.Order

object opening {

  def all(openings: PuzzleOpeningCollection, order: Order)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Puzzles by openings",
      moreCss = cssTag("puzzle.page")
    )(
      main(cls := "page-menu")(
        bits.pageMenu("openings"),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(
            h1("Puzzles by openings"),
            orderSelect(order)
          ),
          div(cls := "puzzle-themes")(
            treeOf(openings treeList order),
            theme.info
          )
        )
      )
    )

  private[puzzle] def listOf(openings: List[PuzzleOpening.WithCount])(implicit ctx: Context) =
    div(cls := "puzzle-openings__list")(openings map { op =>
      a(cls := "puzzle-openings__link", href := routes.Puzzle.show(op.opening.key.value))(
        h3(
          op.opening.name,
          em(op.count.localize)
        )
      )
    })

  private def treeOf(openings: PuzzleOpening.TreeList)(implicit ctx: Context) =
    div(cls := "puzzle-openings")(openings map { case ((family, count), openings) =>
      val familyKey = PuzzleOpening.nameToKey(family.name)
      div(cls := "puzzle-openings__tree__family")(
        h2(
          a(href := routes.Puzzle.show(familyKey.value))(family.name),
          em(count.localize)
        ),
        div(cls := "puzzle-openings__list")(
          openingLink(familyKey, "All variations", count),
          openings.map { op =>
            openingLink(op.opening.key, op.opening.variation.fold("Other variations")(_.name), op.count)
          }
        )
      )
    })

  private def openingLink(key: PuzzleOpening.Key, name: PuzzleOpening.Name, count: PuzzleOpening.Count) =
    a(
      cls  := "puzzle-openings__link",
      href := routes.Puzzle.show(key.value)
    )(
      h3(name, em(count))
    )

  def orderSelect(order: Order)(implicit ctx: Context) = {
    views.html.base.bits.mselect(
      "orders",
      span(order.name()),
      Order.all map { o =>
        a(href := routes.Puzzle.openings(o.key), cls := (order == o).option("current"))(o.name())
      }
    )
  }
}
