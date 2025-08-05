package lila.core
package fide

import _root_.chess.{ FideId, PlayerName, PlayerTitle }
import _root_.chess.rating.{ Elo, KFactor }
import lila.core.userId.UserId

enum FideTC:
  case standard, rapid, blitz

object Federation:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  type Name = String
  type ByFideIds = Map[FideId, Id]
  type NamesOf = List[FideId] => Fu[Map[Federation.Id, Federation.Name]]
  type FedsOf = List[FideId] => Fu[Federation.ByFideIds]

  case class Stats(rank: Int, nbPlayers: Int, top10Rating: Int)

trait Player:
  def id: FideId
  def name: PlayerName
  def fed: Option[Federation.Id]
  def title: Option[PlayerTitle]
  def year: Option[Int]
  def ratingOf(tc: FideTC): Option[Elo]
  def kFactorOf(tc: FideTC): KFactor
  def ratingsMap: Map[FideTC, Elo]

type PlayerToken = String
type GuessPlayer = (Option[FideId], Option[PlayerName], Option[PlayerTitle]) => Fu[Option[Player]]
type GetPlayer = FideId => Fu[Option[Player]]
type GetPlayerFollowers = FideId => Fu[Set[UserId]]

type Tokenize = String => PlayerToken

// FIDE's weird way of not supporting unicode
object diacritics:
  private val replacements = List(
    "ö" -> "oe",
    "ø" -> "o",
    "å" -> "aa",
    "ä" -> "ae",
    "ü" -> "ue",
    "ß" -> "ss"
  )
  def remove(name: String): String =
    replacements.foldLeft(name):
      case (name, (k, v)) =>
        if name.contains(k) then name.replaceAll(k, v) else name
