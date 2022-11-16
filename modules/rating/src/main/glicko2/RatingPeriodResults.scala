package lila.rating.glicko2

import scala.collection.mutable.Set
import java.util.ArrayList
import scala.jdk.CollectionConverters.*

// rewrite from java https://github.com/goochjs/glicko2
abstract class RatingPeriodResults[R <: Result](participants: Set[Rating] = Set.empty):

  val results = new ArrayList[R]()

  /** Get a list of the results for a given player.
    */
  def getResults(player: Rating): List[R] = results.asScala.filter(_ participated player).toList

  /** Get all the participants whose results are being tracked.
    */
  def getParticipants: collection.immutable.Set[Rating] = results.asScala.flatMap(_.players).toSet

  /** Add a participant to the rating period, e.g. so that their rating will still be calculated even if they
    * don't actually compete.
    */
  def addParticipants(rating: Rating) = participants.add(rating)

class GameRatingPeriodResults(participants: Set[Rating] = Set.empty)
    extends RatingPeriodResults[GameResult]():

  def addWin(winner: Rating, loser: Rating) =
    results.add(new GameResult(winner, loser, false))

  def addDraw(player1: Rating, player2: Rating) =
    results.add(new GameResult(player1, player2, true))

class FloatingRatingPeriodResults(participants: Set[Rating] = Set.empty)
    extends RatingPeriodResults[FloatingResult]():

  def addScore(player: Rating, opponent: Rating, score: Float) =
    results.add(new FloatingResult(player, opponent, score))
