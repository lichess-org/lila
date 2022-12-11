package lila.study
package actorApi

import scala.concurrent.Promise

case class StartStudy(studyId: StudyId)
case class SaveStudy(study: Study)
case class SetTag(chapterId: StudyChapterId, name: String, value: String):
  def tag = chess.format.pgn.Tag(name, lila.common.String.fullCleanUp(value) take 140)
case class ExplorerGame(ch: StudyChapterId, path: String, gameId: GameId, insert: Boolean):
  def chapterId = ch
  val position  = Position.Ref(chapterId, Path(path))

case class Who(u: UserId, sri: lila.socket.Socket.Sri)
case class RelayToggle(studyId: StudyId, v: Boolean, who: Who)
case class IsOfficialRelay(studyId: StudyId, promise: Promise[Boolean])
