package lila.player

import reactivemongo.api.bson.Macros.Annotations.Key
import java.text.Normalizer
import chess.FideId

enum FideTC:
  case Standard, Rapid, Blitz

case class FidePlayer(
    @Key("_id") id: FideId,
    name: PlayerName,
    token: PlayerToken,
    fed: Option[String],
    title: Option[UserTitle],
    standard: Option[Int],
    rapid: Option[Int],
    blitz: Option[Int],
    fetchedAt: Instant
):
  def ratingOf(tc: FideTC) = tc match
    case FideTC.Standard => standard
    case FideTC.Rapid    => rapid
    case FideTC.Blitz    => blitz

object FidePlayer:
  case class TokenTitle(token: PlayerToken, title: Option[UserTitle])
  private val nonLetterRegex = """[^a-zA-Z0-9\s]+""".r
  private val splitRegex     = """\W""".r
  def tokenize(name: PlayerName): PlayerToken =
    splitRegex
      .split:
        Normalizer
          .normalize(name.trim, Normalizer.Form.NFD)
          .replaceAllIn(nonLetterRegex, "")
          .toLowerCase
      .toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .distinct
      .sorted
      .mkString(" ")
