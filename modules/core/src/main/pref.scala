package lila.core
package pref

import lila.core.user.User
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
  val resizeHandle: Int
  val theme: String
  val pieceSet: String
  val usingAltSocket: Option[Boolean]

  def hasKeyboardMove: Boolean
  def hasVoice: Boolean
  def hideRatingsInGame: Boolean
  def showRatings: Boolean
  def animationMillis: Int
  def animationMillisForSpeedPuzzles: Int
  def pieceNotationIsLetter: Boolean
  def currentBg: String

trait PrefApi:
  def followable(userId: UserId): Fu[Boolean]
  def mentionableIds(userIds: Set[UserId]): Fu[Set[UserId]]
  def getMessage(userId: UserId): Fu[Int]
  def getInsightShare(userId: UserId): Future[Int]
  def getChallenge(userId: UserId): Future[Int]
  def getStudyInvite(userId: UserId): Future[Int]
  def isolate(user: User): Funit

object Message:
  val NEVER  = 1
  val FRIEND = 2
  val ALWAYS = 3

object InsightShare:
  val NOBODY    = 0
  val FRIENDS   = 1
  val EVERYBODY = 2

object Challenge:
  val NEVER      = 1
  val RATING     = 2
  val FRIEND     = 3
  val REGISTERED = 4
  val ALWAYS     = 5

object StudyInvite:
  val NEVER  = 1
  val FRIEND = 2
  val ALWAYS = 3
