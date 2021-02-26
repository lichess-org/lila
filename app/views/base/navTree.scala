package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object navTree {

  sealed trait Node {
    val id: String
    val name: Frag
  }
  case class Branch(id: String, name: Frag, children: List[Node]) extends Node
  case class Leaf(id: String, name: Frag, content: Frag)          extends Node

  def renderNode(node: Node, parent: Option[Node])(implicit ctx: Context): Frag =
    node match {
      case Leaf(_, _, content) =>
        List(
          div(makeId(node.id), cls := "node leaf")(
            h2(parent map goBack, node.name),
            div(cls := "content")(content)
          )
        )
      case b @ Branch(id, _, children) =>
        frag(
          div(makeId(node.id), cls := s"node branch $id")(
            h2(parent map goBack, node.name),
            div(cls := "links")(
              children map { child =>
                a(makeLink(child.id))(child.name)
              }
            )
          ),
          children map { renderNode(_, b.some) }
        )
    }

  private def makeId(id: String) = st.id := s"help-$id"

  private def makeLink(id: String) = href := s"#help-$id"

  private def goBack(parent: Node): Frag =
    a(makeLink(parent.id), cls := "back", dataIcon := "I", title := "Go back")
}
