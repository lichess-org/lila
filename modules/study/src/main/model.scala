package lila.study

import chess.format.UciPath
import chess.variant.Variant
import lila.tree.Branch

private case class ExplorerGame(ch: StudyChapterId, path: UciPath, gameId: GameId, insert: Boolean):
  def chapterId = ch
  val position = Position.Ref(chapterId, path)

case class Who(u: UserId, sri: lila.core.socket.Sri):
  def myId = u.into(MyId)
case class RelayToggle(studyId: StudyId, v: Boolean, who: Who)
case class Kick(studyId: StudyId, userId: UserId, who: MyId)
case class BecomeStudyAdmin(studyId: StudyId, me: Me)
case class IsOfficialRelay(studyId: StudyId, promise: Promise[Boolean])

case class AddNode(
    studyId: StudyId,
    positionRef: Position.Ref,
    node: Variant => Either[chess.ErrorStr, Branch],
    opts: MoveOpts,
    relay: Option[Chapter.Relay] = None
)(using val who: Who)
