package lila.hub
package tournament

import play.api.i18n.Lang

trait GetTourName:
  def sync(id: TourId)(using Lang): Option[String]
  def preload(ids: Iterable[TourId])(using Lang): Funit

trait Tournament:
  def nbPlayers: Int

trait TournamentApi:
  def allCurrentLeadersInStandard: Fu[Map[Tournament, List[UserId]]]

object leaderboard:

  opaque type Ratio = Double
  object Ratio extends OpaqueDouble[Ratio]:
    extension (a: Ratio) def percent = (a.value * 100).toInt.atLeast(1)

  trait Api:
    def timeRange(userId: UserId, range: TimeInterval): Fu[List[Entry]]

  trait Entry:
    def id: TourPlayerId
    def userId: UserId
    def tourId: TourId
    def nbGames: Int
    def score: Int
    def rank: Rank
    def rankRatio: Ratio // ratio * rankRatioMultiplier. function of rank and tour.nbPlayers. less is better.
    // freq: Option[Schedule.Freq]
    // speed: Option[Schedule.Speed]
    // perf: PerfType
    def date: Instant
