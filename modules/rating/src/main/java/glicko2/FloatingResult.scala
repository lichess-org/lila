package org.goochjs.glicko2

import scala.jdk.CollectionConverters.*

// score from 0 (opponent wins) to 1 (player wins)
class FloatingResult(player: Rating, opponent: Rating, score: Float) extends Result:

  def getScore(p: Rating) = if (p == player) score else 1 - score

  def getOpponent(p: Rating) = if (p == player) opponent else player

  def participated(p: Rating) = p == player || p == opponent

  def getPlayers() = List(player, opponent).asJava
