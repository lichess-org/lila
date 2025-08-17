package lila.gathering

import play.api.data.Forms.*
import play.api.data.Mapping
import scalalib.model.Days
import chess.IntRating

import lila.common.Form.{ *, given }
import lila.core.team.LightTeam
import lila.gathering.Condition.*

object ConditionForm:

  val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
  val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}").map:
    case (0, _) => (0, "No restriction")
    case x => x

  val nbRatedGame: Mapping[Option[NbRatedGame]] = optional(
    mapping(
      "nb" -> numberIn(nbRatedGameChoices)
    )(NbRatedGame.apply)(_.nb.some)
  ).transform(_.filter(_.nb > 0), identity)

  val maxRatings =
    List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)

  val maxRatingChoices = ("", "No restriction") ::
    options(maxRatings, "Max rating of %d").toList.map { (k, v) => k.toString -> v }

  val maxRating = optional:
    mapping("rating" -> numberIn(maxRatings).into[IntRating])(MaxRating.apply)(_.rating.some)

  val minRatings =
    List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600)

  val minRatingChoices = ("", "No restriction") ::
    options(minRatings, "Min rating of %d").toList.map { (k, v) => k.toString -> v }

  val minRating = optional:
    mapping("rating" -> numberIn(minRatings).into[IntRating])(MinRating.apply)(_.rating.some)

  val titled: Mapping[Option[Titled.type]] =
    optional(boolean).transform(_.contains(true).option(Titled), _.isDefined.option(true))

  val bots: Mapping[Option[Bots]] =
    optional(boolean).transform(_.contains(true).option(Bots(true)), _.exists(_.allowed).option(true))

  val accountAges = List(1, 3, 7, 14, 30, 60, 90, 180, 365, 365 * 2, 365 * 3)

  val accountAgeChoices =
    def pluralize(s: String, n: Int) = s"$n $s${if n != 1 then "s" else ""}"
    ("", "No restriction") :: options(
      accountAges,
      format = days =>
        if days < 30 then pluralize("day", days)
        else if days < 365 then pluralize("month", days / 30)
        else pluralize("year", days / 365)
    ).toList

  val accountAge: Mapping[Option[AccountAge]] = optional:
    numberIn(accountAges).into[Days].transform(AccountAge.apply, _.days)

  def teamMember(leaderTeams: List[LightTeam]): Mapping[Option[TeamMember]] = optional:
    mapping(
      "teamId" -> of[TeamId].verifying(id => leaderTeams.exists(_.id == id))
    )(id => TeamMember(id, leaderTeams.find(_.id == id).err(s"Team $id not found").name))(_.teamId.some)

  def allowList = optional:
    nonEmptyText(maxLength = 100_1000)
      .transform[String](_.replace(',', '\n'), identity)
      .transform[String](_.linesIterator.map(_.trim).filter(_.nonEmpty).distinct.mkString("\n"), identity)
      .verifying("5000 usernames max", _.count('\n' == _) <= 5_000)
      .transform(AllowList(_), _.value)
