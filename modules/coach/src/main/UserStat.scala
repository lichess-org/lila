package lila.coach

import org.joda.time.DateTime

case class UserStat(
    _id: String, // user ID
    openings: UserStat.Openings,
    nbGames: Int,
    nbAnalysis: Int,
    date: DateTime) {

  def id = _id

  def withGame(pov: lila.game.Pov, analysis: Option[lila.analyse.Analysis]) = copy(
    nbGames = nbGames + 1,
    nbAnalysis = nbAnalysis + analysis.isDefined.fold(1, 0),
    openings = openings withGame pov
  )

  def isFresh = nbGames < 100 || {
    DateTime.now minusDays 1 isBefore date
  }
}

object UserStat {

  type OpeningMap = Map[String, Int]

  case class Openings(
      white: OpeningMap,
      black: OpeningMap) {

    def withGame(p: lila.game.Pov) = p.game.opening.map(_.code).fold(this) { code =>
      copy(
        white = if (p.color.white) openingWithCode(white, code) else white,
        black = if (p.color.black) openingWithCode(black, code) else black)
    }

    private def openingWithCode(opening: OpeningMap, code: String) =
      opening + (code -> opening.get(code).fold(1)(1+))
  }

  def apply(id: String): UserStat = UserStat(
    _id = id,
    openings = Openings(Map.empty, Map.empty),
    nbGames = 0,
    nbAnalysis = 0,
    date = DateTime.now)
}
