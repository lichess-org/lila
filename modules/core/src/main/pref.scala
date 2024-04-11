package lila.core
package pref

import lila.core.userId.UserId

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

trait PrefApi:
  def followable(userId: UserId): Fu[Boolean]
  def getMessage(userId: UserId): Fu[Int]

object Message:
  val NEVER  = 1
  val FRIEND = 2
  val ALWAYS = 3
