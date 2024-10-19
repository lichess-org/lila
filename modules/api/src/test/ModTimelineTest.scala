package lila.api

import lila.shutup.PublicLine
import lila.core.shutup.PublicSource as Source
import lila.playban.TempBan

class ModTimelineTest extends munit.FunSuite:

  import ModTimeline.*

  val now = nowInstant

  def line(text: String, source: String) = PublicLine(text, Source.longNotation.read(source).get, now)
  val l1                                 = line("linguine", "simul/aaa")
  val l2                                 = line("fusilli", "simul/aaa")
  val l3                                 = line("linguine", "relay/bbb")
  val l4                                 = line("bucatini", "study/ccc")
  val l5                                 = line("rigatoni", "study/ccc")
  val ban1                               = TempBan(now.minusDays(10), 5)
  val ban2                               = TempBan(now.minusDays(5), 10)
  val ban3                               = TempBan(now.minusDays(1), 15)

  test("merge empty"):
    assertEquals(aggregateEvents(Nil), Nil)

  test("public line merge"):
    assertEquals(PublicLine.merge(l1, l1), l1.copy(text = "linguine|linguine").some)
    assertEquals(PublicLine.merge(l1, l2), l1.copy(text = "linguine|fusilli").some)
    assertEquals(PublicLine.merge(l2, l3), none)

  test("merge same line"):
    assertEquals(aggregateEvents(List(l1)), List(l1))
    assertEquals(aggregateEvents(List(l1, l1)), List(l1.copy(text = "linguine|linguine")))

  test("merge same source lines"):
    assertEquals(aggregateEvents(List(l1, l2)), List(l1.copy(text = "linguine|fusilli")))
    assertEquals(aggregateEvents(List(l2, l1)), List(l1.copy(text = "fusilli|linguine")))
    assertEquals(aggregateEvents(List(l1, l2, l1)), List(l1.copy(text = "linguine|fusilli|linguine")))

  test("merge different source lines"):
    assertEquals(aggregateEvents(List(l1, l3)), List(l1, l3))
    assertEquals(aggregateEvents(List(l4, l1, l3)), List(l4, l1, l3))

  test("merge consecutive lines"):
    assertEquals(
      aggregateEvents(List(l4, l1, l1, l2, l3)),
      List(l4, l1.copy(text = "linguine|linguine|fusilli"), l3)
    )
    assertEquals(
      aggregateEvents(List(l4, l5, l1, l2, l1, l3)),
      List(l4.copy(text = "bucatini|rigatoni"), l1.copy(text = "linguine|fusilli|linguine"), l3)
    )

  test("merge mixed lines"):
    assertEquals(
      aggregateEvents(List(l1, l4, l1)),
      List(l1.copy(text = "linguine|linguine"), l4)
    )
    assertEquals(
      aggregateEvents(List(l1, l4, l2, l3, l5)),
      List(l1.copy(text = "linguine|fusilli"), l4.copy(text = "bucatini|rigatoni"), l3)
    )

  test("merge mixed events"):
    assertEquals(
      aggregateEvents(List(ban1, ban2)),
      List(ban1, ban2)
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, ban2)),
      List(ban1, l1, ban2)
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, l1)),
      List(ban1, l1.copy(text = "linguine|linguine"))
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, l1, ban2)),
      List(ban1, l1.copy(text = "linguine|linguine"), ban2)
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, l4, l1, ban2)),
      List(ban1, l1.copy(text = "linguine|linguine"), l4, ban2)
    )
    assertEquals(
      aggregateEvents(List(l1, ban1, l4, ban2, l2, l3, l5, ban3, ban3)),
      List(
        l1.copy(text = "linguine|fusilli"),
        ban1,
        l4.copy(text = "bucatini|rigatoni"),
        ban2,
        l3,
        ban3,
        ban3
      )
    )
