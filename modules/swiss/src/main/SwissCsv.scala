package lila.swiss

import akka.stream.scaladsl.Source

object SwissCsv:

  def apply(results: Source[SwissPlayer.WithUserAndRank, ?]): Source[String, ?] =
    Source(
      List(
        toCsv(
          "Rank",
          "Title",
          "Username",
          "Rating",
          "Points",
          "Tie Break",
          "Performance"
        )
      )
    ).concat(results.map(apply))

  def apply(p: SwissPlayer.WithUserAndRank): String = toCsv(
    p.rank.toString,
    p.user.title.so(_.toString),
    p.user.name.value,
    p.player.rating.toString,
    p.player.points.value.toString,
    p.player.tieBreak.value.toString,
    p.player.performance.so(_.value.toString)
  )

  private def toCsv(values: String*) = values.mkString(",")
