package lila.report

import lila.db.dsl.*

private object ReportQueueMonitor:

  def push(reportColl: Coll)(using Executor): Unit =
    reportColl
      .aggregateList(50, _.sec): framework =>
        import framework.*
        Match(bdoc("open" -> true, "score" -> bdoc("$gte" -> 20))) -> List(
          Group(
            barr(
              "$room",
              bdoc(
                "$min" -> barr(
                  80,
                  bdoc("$multiply" -> barr(20, bdoc("$floor" -> bdoc("$divide" -> barr("$score", 20)))))
                )
              )
            )
          )("nb" -> SumAll),
          Project(
            bdoc(
              "_id" -> 0,
              "room" -> bdoc("$first" -> "$_id"),
              "score" -> bdoc("$last" -> "$_id"),
              "nb" -> 1
            )
          )
        )
      .map: docs =>
        for
          doc <- docs
          room <- doc.string("room")
          nb <- doc.int("nb")
          score <- doc.int("score")
        do lila.mon.mod.queueStatus(room, score).update(nb)
