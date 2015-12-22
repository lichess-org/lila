package lila.perfStat

import lila.rating.PerfType

import org.joda.time.DateTime

case class PerfStat(
  _id: String, // userId/perfId
  userId: String,
  perfType: PerfType,
  highest: RatingAt,
  lowest: RatingAt,
  bestWin: Result,
  worstLoss: Result,
  opAvg: Double,
  winningStreak: Int,
  losingStreak: Int,
  playStreak: PlayStreak)

case class PlayStreak(
  nb: Int,
  minutes: Int)

case class Count(
  all: Int,
  rated: Int,
  win: Int,
  loss: Int,
  draw: Int)

case class RatingAt(
  int: Int,
  at: DateTime,
  gameId: String)

case class Result(
  opInt: Int,
  opId: String,
  at: DateTime,
  gameId: String)
