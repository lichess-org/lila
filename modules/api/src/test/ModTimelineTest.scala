package lila.api

import monocle.syntax.all.*

import lila.core.shutup.PublicLine
import lila.core.chat.PublicSource
import lila.playban.TempBan

class ModTimelineTest extends munit.FunSuite:

  import ModTimeline.*
  import lila.shutup.PublicLine.merge.sep

  val now = nowInstant

  def line(text: String, source: String) =
    ReportLineFlag(None, PublicLine(text, PublicSource.longNotation.read(source).get, now))
  val l1 = line("linguine", "simul/aaa")
  val l2 = line("fusilli", "simul/aaa")
  val l3 = line("linguine", "relay/bbb")
  val l4 = line("bucatini", "study/ccc")
  val l5 = line("rigatoni", "study/ccc")
  val ban1 = TempBan(now.minusDays(10), 5, none)
  val ban2 = TempBan(now.minusDays(5), 10, none)
  val ban3 = TempBan(now.minusDays(1), 15, none)
  def bans(bs: TempBan*) = PlayBans(NonEmptyList.fromListUnsafe(bs.toList))
  given Conversion[TempBan, Event] = bans(_)

  test("merge empty"):
    assertEquals(aggregateEvents(Nil), Nil)

  test("public line merge"):
    assertEquals(l1.merge(l1), l1.some)
    assertEquals(l1.merge(l2), l1.focus(_.line.text).replace(s"linguine${sep}fusilli").some)
    assertEquals(l2.merge(l3), none)

  test("merge same line"):
    assertEquals(aggregateEvents(List(l1)), List(l1))
    assertEquals(aggregateEvents(List(l1, l1)), List(l1))

  test("merge same source lines"):
    assertEquals(aggregateEvents(List(l1, l2)), List(l1.focus(_.line.text).replace(s"linguine${sep}fusilli")))
    assertEquals(aggregateEvents(List(l2, l1)), List(l1.focus(_.line.text).replace(s"fusilli${sep}linguine")))
    assertEquals(
      aggregateEvents(List(l1, l2, l1)),
      List(l1.focus(_.line.text).replace(s"linguine${sep}fusilli"))
    )

  test("merge different source lines"):
    assertEquals(aggregateEvents(List(l1, l3)), List(l1, l3))
    assertEquals(aggregateEvents(List(l4, l1, l3)), List(l4, l1, l3))

  test("merge consecutive lines"):
    assertEquals(
      aggregateEvents(List(l4, l1, l1, l2, l3)),
      List(l4, l1.focus(_.line.text).replace(s"linguine${sep}fusilli"), l3)
    )
    assertEquals(
      aggregateEvents(List(l4, l5, l1, l2, l1, l3)),
      List(
        l4.focus(_.line.text).replace(s"bucatini${sep}rigatoni"),
        l1.focus(_.line.text).replace(s"linguine${sep}fusilli"),
        l3
      )
    )

  test("merge mixed lines"):
    assertEquals(
      aggregateEvents(List(l1, l4, l1)),
      List(l1, l4)
    )
    assertEquals(
      aggregateEvents(List(l1, l4, l2, l3, l5)),
      List(
        l1.focus(_.line.text).replace(s"linguine${sep}fusilli"),
        l4.focus(_.line.text).replace(s"bucatini${sep}rigatoni"),
        l3
      )
    )

  test("merge mixed events"):
    assertEquals(
      aggregateEvents(List(ban1, ban2)),
      List(bans(ban2, ban1))
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, ban2)),
      List[Event](bans(ban2, ban1), l1)
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, l1)),
      List[Event](ban1, l1)
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, l1, ban2)),
      List[Event](bans(ban2, ban1), l1)
    )
    assertEquals(
      aggregateEvents(List(ban1, l1, l4, l1, ban2)),
      List[Event](bans(ban2, ban1), l1, l4)
    )
    assertEquals(
      aggregateEvents(List(l1, ban1, l4, ban2, l2, l3, l5, ban3, ban3)),
      List[Event](
        l1.focus(_.line.text).replace(s"linguine${sep}fusilli"),
        bans(ban3, ban3, ban2, ban1),
        l4.focus(_.line.text).replace(s"bucatini${sep}rigatoni"),
        l3
      )
    )
