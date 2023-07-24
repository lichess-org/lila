package lila.study

case class Position(chapter: Chapter, path: Path) {

  def ref = Position.Ref(chapter.id, path)

  def node: Option[RootOrNode] = chapter.root nodeAt path

  override def toString = ref.toString
}

case object Position {

  case class Ref(chapterId: Chapter.Id, path: Path) {

    def encode = s"$chapterId $path"

    def +(node: Node) = copy(path = path + node)

    def withPath(p: Path) = copy(path = p)
  }

  object Ref {

    def decode(str: String): Ref =
      str.split(' ') match {
        case Array(chapterId, path) => Ref(Chapter.Id(chapterId), Path(path))
        case Array(chapterId)       => Ref(Chapter.Id(chapterId), Path.root)
        case _                      => Ref(Chapter.Id(""), Path.root)
      }
  }
}
