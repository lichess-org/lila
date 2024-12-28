package lila.study

import chess.format.UciPath
import lila.tree.Branch

case class SaveStudy(study: Study)
case class SetTag(chapterId: StudyChapterId, name: String, value: String):
  def tag = chess.format.pgn.Tag(name, lila.common.String.fullCleanUp(value).take(140))
case class ExplorerGame(ch: StudyChapterId, path: UciPath, gameId: GameId, insert: Boolean):
  def chapterId = ch
  val position  = Position.Ref(chapterId, path)

case class Who(u: UserId, sri: lila.core.socket.Sri):
  def myId = u.into(MyId)
case class RelayToggle(studyId: StudyId, v: Boolean, who: Who)
case class Kick(studyId: StudyId, userId: UserId, who: MyId)
case class BecomeStudyAdmin(studyId: StudyId, me: Me)
case class IsOfficialRelay(studyId: StudyId, promise: Promise[Boolean])

case class AddNode(
    studyId: StudyId,
    positionRef: Position.Ref,
    node: Branch,
    opts: MoveOpts,
    relay: Option[Chapter.Relay] = None
)(using val who: Who)
