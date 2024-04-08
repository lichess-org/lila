package lila.core
package pref

trait Pref:
  val id: UserId
  val coords: Int
  val keyboardMove: Int
  val voice: Option[Int]
  val rookCastle: Int
  val animation: Int
  val destination: Boolean
  val moveEvent: Int
  val highlight: Boolean
  val is3d: Boolean

  def showRatings: Boolean
  def animationMillis: Int
  def animationMillisForSpeedPuzzles: Int
