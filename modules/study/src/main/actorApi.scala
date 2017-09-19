package lila.study
package actorApi

case class StartStudy(studyId: Study.Id)
case class SaveStudy(study: Study)
case class SetTag(chapterId: Chapter.Id, name: String, value: String) {
  def tag = chess.format.pgn.Tag(name, value take 140)
}
case class ExplorerGame(chapterId: Chapter.Id, path: String, gameId: String, insert: Boolean)
