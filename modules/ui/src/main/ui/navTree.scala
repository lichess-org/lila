package lila.ui

import ScalatagsTemplate.{ *, given }

object navTree:

  enum Node:
    val id: String
    val name: Frag
    val showBack: Boolean
    case Branch(
        id: String,
        name: Frag,
        children: List[Node],
        content: Option[Frag] = None,
        showLinks: Boolean = true,
        showBack: Boolean = true
    )
    case Leaf(id: String, name: Frag, content: Frag, showBack: Boolean = true)

  def renderNode(node: Node, parent: Option[Node], forceLtr: Boolean = false): Frag =
    node match
      case Node.Leaf(id, name, content, showBack) =>
        List(
          div(makeId(id), cls := "node leaf")(
            h2(backLink(parent, showBack, forceLtr), name),
            div(cls := "content")(content)
          )
        )
      case b @ Node.Branch(id, name, children, content, showLinks, showBack) =>
        frag(
          div(makeId(id), cls := s"node branch $id")(
            h2(backLink(parent, showBack, forceLtr), name),
            content.map { div(cls := "content")(_) },
            Option.when(showLinks):
              div(cls := "links")(
                children.map { child =>
                  a(makeLink(child.id))(child.name)
                }
              )
          ),
          children.map { renderNode(_, b.some, forceLtr) }
        )

  private def makeId(id: String) = st.id := s"help-$id"

  private def makeLink(id: String) = href := s"#help-$id"

  private def backLink(parent: Option[Node], showBack: Boolean, forceLtr: Boolean): Option[Frag] =
    parent
      .filter(_ => showBack)
      .map(p =>
        a(
          makeLink(p.id),
          cls := List("back text" -> true, "no-mirror" -> forceLtr),
          dataIcon := Icon.LessThan,
          title := "Go back"
        )
      )
