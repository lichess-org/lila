package lila.tournament

import org.apache.pekko.stream.scaladsl.Source

object TournamentCsv:

  def apply(results: Source[Player.Result, ?]): Source[String, ?] =
    Source(
      List(
        toCsv(
          "Rank",
          "Title",
          "Username",
          "Rating",
          "Score",
          "Performance",
          "Team",
          "Sheet"
        )
      )
    ).concat(results.map(playerLine))

  private def playerLine(p: Player.Result): String =
    import p.*
    toCsv(
      rank.toString,
      lightUser.title.so(_.toString),
      lightUser.name.value,
      player.rating.toString,
      player.score.toString,
      player.performance.so(_.toString),
      ~player.team.map(_.value),
      sheet.so(_.scoresToString)
    )

  private def toCsv(values: String*) = values.mkString(",")
