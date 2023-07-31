package lila.study

import lila.tree.Node.{ Comment, Comments, Shapes }

import lila.tree.{ Branch, Branches, Root, Metas, NewTree, NewBranch, NewRoot, Node }
import Helpers.*

class NewChapterTest extends munit.FunSuite:

  test("yo"):
    assertEquals(1, 1)


object NewChapterTest:
  extension(chapter: Chapter)
    def toNewChapter: NewChapter =
      NewChapter(
        _id = chapter._id,
        studyId = chapter.studyId,
        name = chapter.name,
        setup = chapter.setup,
        root = chapter.root.toNewRoot,
        tags = chapter.tags,
        order = chapter.order,
        ownerId = chapter.ownerId,
        conceal = chapter.conceal,
        practice = chapter.practice,
        gamebook = chapter.gamebook,
        description = chapter.description,
        relay = chapter.relay,
        serverEval = chapter.serverEval,
        createdAt = chapter.createdAt,
      )
