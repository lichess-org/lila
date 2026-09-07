package lila.report

import lila.db.dsl.*

private object ReportQueueMonitor:

  def push(reportColl: Coll)(using Executor): Unit =
    reportColl
      .aggregateList(50, _.sec): framework =>
        import framework.*
        Match($doc("open" -> true, "score" -> $doc("$gte" -> 20))) -> List(
          Group(
            $arr(
              "$room",
              $doc(
                "$min" -> $arr(
                  80,
                  $doc("$multiply" -> $arr(20, $doc("$floor" -> $doc("$divide" -> $arr("$score", 20)))))
                )
              )
            )
          )("nb" -> SumAll),
          Project(
            $doc(
              "_id" -> 0,
              "room" -> $doc("$first" -> "$_id"),
              "score" -> $doc("$last" -> "$_id"),
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
