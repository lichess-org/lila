package lila.tournament

import akka.stream.scaladsl.Source

object TournamentCsv {

  def apply(results: Source[Player.Result, _]): Source[String, _] =
    Source(
      List(
        toCsv(
          "Rank",
          "Title",
          "Username",
          "Rating",
          "Score",
          "Performance",
          "Team"
        )
      )
    ) concat
      results.map(apply)

  def apply(p: Player.Result): String = p match {
    case Player.Result(player, user, rank) =>
      toCsv(
        rank.toString,
        ~user.title,
        user.name,
        player.rating.toString,
        player.score.toString,
        player.performanceOption.??(_.toString),
        ~player.team
      )
  }

  private def toCsv(values: String*) = values mkString ","
}
