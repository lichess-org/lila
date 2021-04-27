package lila.study
package actorApi

case class StartStudy(studyId: Study.Id)
case class SaveStudy(study: Study)
case class SetTag(chapterId: Chapter.Id, name: String, value: String) {
  def tag = chess.format.pgn.Tag(name, value take 140)
}
case class ExplorerGame(ch: Chapter.Id, path: String, gameId: String, insert: Boolean) {
  def chapterId = ch
  val position  = Position.Ref(chapterId, Path(path))
}

case class Who(u: lila.user.User.ID, sri: lila.socket.Socket.Sri)
case class RelayToggle(studyId: Study.Id, v: Boolean, who: Who)
