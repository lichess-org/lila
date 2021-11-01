package org.goochjs.glicko2

trait Result {

  def getScore(player: Rating): Double

  def getOpponent(player: Rating): Rating
}
