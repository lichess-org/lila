package views.html
package base

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object navTree:

  enum Node:
    val id: String
    val name: Frag
    case Branch(id: String, name: Frag, children: List[Node], content: Option[Frag] = None)
    case Leaf(id: String, name: Frag, content: Frag)

  def renderNode(node: Node, parent: Option[Node], forceLtr: Boolean = false)(using PageContext): Frag =
    node match
      case Node.Leaf(id, name, content) =>
        List(
          div(makeId(id), cls := "node leaf")(
            h2(parent.map(goBack(_, forceLtr)), name),
            div(cls := "content")(content)
          )
        )
      case b @ Node.Branch(id, name, children, content) =>
        frag(
          div(makeId(id), cls := s"node branch $id")(
            h2(parent.map(goBack(_, forceLtr)), name),
            content map { div(cls := "content")(_) },
            div(cls := "links")(
              children map { child =>
                a(makeLink(child.id))(child.name)
              }
            )
          ),
          children map { renderNode(_, b.some, forceLtr) }
        )

  private def makeId(id: String) = st.id := s"help-$id"

  private def makeLink(id: String) = href := s"#help-$id"

  private def goBack(parent: Node, forceLtr: Boolean): Frag =
    a(
      makeLink(parent.id),
      cls      := List("back" -> true, "no-mirror" -> forceLtr),
      dataIcon := licon.LessThan,
      title    := "Go back"
    )
