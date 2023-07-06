package views.html.opening

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.opening.{ OpeningTree, OpeningConfig }

object tree:

  import bits.*

  def apply(root: OpeningTree, config: OpeningConfig)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(none),
      title = trans.opening.txt()
    ) {
      main(cls := "page box box-pad opening opening--tree")(
        index.searchAndConfig(config, "", "tree"),
        search.resultsList(Nil),
        boxTop(
          h1("Chess openings name tree"),
          div(cls := "box__top__actions")(
            a(href := routes.Opening.index())("Opening pages"),
            a(href := s"${routes.UserAnalysis.index}#explorer")("Explorer")
          )
        ),
        div(cls := "opening__tree")(
          renderChildren(root, 1)
        )
      )
    }

  private def renderChildren(node: OpeningTree, level: Int): Frag =
    node.children map { case (op, node) =>
      val fold = level < 4 && node.children.nonEmpty
      val content = frag(
        (if fold then summary else div) (op match
          case (name, None)     => name
          case (name, Some(op)) => a(href := openingUrl(op))(name)
        ),
        renderChildren(node, level + 1)
      )
      if fold then details(content) else content
    }
