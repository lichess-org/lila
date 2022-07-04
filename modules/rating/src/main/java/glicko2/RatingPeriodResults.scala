package org.goochjs.glicko2

import scala.collection.mutable.Set
import java.util.ArrayList
import scala.jdk.CollectionConverters._

/** This class holds the results accumulated over a rating period.
  *
  * @author
  *   Jeremy Gooch
  */
abstract class RatingPeriodResults[R <: Result](participants: Set[Rating] = Set.empty) {

  val results = new ArrayList[R]()

  /** Get a list of the results for a given player.
    */
  def getResults(player: Rating): java.util.List[R] = results.asScala.filter(_ participated player).asJava

  /** Get all the participants whose results are being tracked.
    */
  def getParticipants(): java.util.Set[Rating] = {
    // Run through the results and make sure all players have been pushed into the participants set.
    results.asScala foreach {
      _.getPlayers().asScala foreach participants.add
    }
    participants.asJava
  }

  /** Add a participant to the rating period, e.g. so that their rating will still be calculated even if they
    * don't actually compete.
    */
  def addParticipants(rating: Rating) = participants.add(rating)

  def clear() = {
    results.clear()
  }
}

class GameRatingPeriodResults(participants: Set[Rating] = Set.empty)
    extends RatingPeriodResults[GameResult]() {

  def addWin(winner: Rating, loser: Rating) = {
    results.add(new GameResult(winner, loser))
  }

  def addDraw(player1: Rating, player2: Rating) = {
    results.add(new GameResult(player1, player2, true))
  }
}

class FloatingRatingPeriodResults(participants: Set[Rating] = Set.empty)
    extends RatingPeriodResults[FloatingResult]() {

  def addScore(player: Rating, opponent: Rating, score: Float) = {
    results.add(new FloatingResult(player, opponent, score))
  }
}
