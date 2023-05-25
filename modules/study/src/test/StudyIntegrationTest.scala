package lila.study

import chess.White
import chess.variant.Standard
import chess.format.UciPath
import chess.format.pgn.Tags

import lila.socket.AnaMove
import java.time.Instant
import lila.tree.Root
import lila.tree.Branch

class StudyIntegrationTest extends munit.FunSuite:

  val moves: List[AnaMove] = Nil
  val studyId              = StudyId("studyId")
  val chapterId            = StudyChapterId("chapterId")
  val userId               = UserId("nt9")
  val members              = StudyMembers(Map(userId -> StudyMember(userId, StudyMember.Role.Write)))
  val studyInstant         = Instant.ofEpochSecond(1685031726L)
  val position             = Position.Ref(chapterId, UciPath.root)

  val root = Root.default(Standard)
  val chapter = Chapter(
    chapterId,
    studyId,
    StudyChapterName("chapterName"),
    Chapter.Setup(None, Standard, White, None),
    root,
    Tags.empty,
    0,
    userId,
    None,
    None,
    None,
    None,
    None,
    None,
    studyInstant
  )

  // combined of StudySocket.moveOrDrop & StudyApi.addNode
  def addNode(chapter: Chapter, move: AnaMove): Chapter =
    val rawNode = move.branch.getOrElse(throw Exception("no branch"))
    chapter.addNode(rawNode.withoutChildren, move.path, None).get

  test("moves"):
    val output = moves.foldLeft(chapter): (chapter, move) =>
      addNode(chapter, move)
    assertEquals(output.root, root)
