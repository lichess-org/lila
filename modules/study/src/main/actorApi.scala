package lila.study
package actorApi

case class SaveStudy(study: Study)
case class RemoveStudy(id: Study.ID)
case class SetTag(chapterId: Chapter.ID, name: String, value: String) {
  def tag = chess.format.pgn.Tag(name, value take 140)
}
