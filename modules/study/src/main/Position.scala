package lila.study

case class Position(chapter: Chapter, path: Path)

case object Position {

  case class Ref(chapterId: Chapter.ID, path: Path) {

    def encode = s"$chapterId $path"
  }

  object Ref {

    def decode(str: String): Option[Ref] = str.split(' ') match {
      case Array(chapterId, path) => Ref(chapterId, Path(path)).some
      case Array(chapterId)       => Ref(chapterId, Path.root).some
      case _                      => none
    }
  }
}
