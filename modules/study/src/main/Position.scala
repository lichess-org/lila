package lila.study

import chess.format.UciPath

case class Position(chapter: Chapter, path: UciPath):

  def ref = Position.Ref(chapter.id, path)

  def node: Option[RootOrNode] = chapter.root nodeAt path

  override def toString = ref.toString

case object Position:

  case class Ref(chapterId: StudyChapterId, path: UciPath):

    def encode = s"$chapterId $path"

    def +(node: Node) = copy(path = path + node.id)

    def withPath(p: UciPath) = copy(path = p)

  private[study] object Ref:

    def decode(str: String): Option[Ref] =
      str.split(' ') match
        case Array(chapterId, path) => Ref(StudyChapterId(chapterId), UciPath(path)).some
        case Array(chapterId)       => Ref(StudyChapterId(chapterId), UciPath.root).some
        case _                      => none
