package lila.fide

import chess.{ FideId, PlayerName, PlayerTitle }
import reactivemongo.api.bson.Macros.Annotations.Key

import java.text.Normalizer
import lila.core.fide.{ FideTC, PlayerToken, Tokenize }

case class FidePlayer(
    @Key("_id") id: FideId,
    name: PlayerName,
    token: PlayerToken,
    fed: Option[lila.core.fide.Federation.Id],
    title: Option[PlayerTitle],
    standard: Option[Int],
    rapid: Option[Int],
    blitz: Option[Int],
    year: Option[Int],
    inactive: Option[Boolean],
    fetchedAt: Instant
) extends lila.core.fide.Player:

  def ratingOf(tc: FideTC): Option[Int] = tc match
    case FideTC.standard => standard
    case FideTC.rapid    => rapid
    case FideTC.blitz    => blitz

  def slug: String = FidePlayer.slugify(name)

  def age: Option[Int] = year.map(nowInstant.date.getYear - _)

object FidePlayer:

  val tokenize: Tokenize =
    val nonLetterRegex = """[^a-zA-Z0-9\s]+""".r
    val splitRegex     = """\W""".r
    str =>
      splitRegex
        .split:
          Normalizer
            .normalize(str.trim, Normalizer.Form.NFD)
            .replaceAllIn(nonLetterRegex, "")
            .toLowerCase
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .distinct
        .sorted
        .mkString(" ")

  val slugify: PlayerName => String =
    val splitAccentRegex = "[\u0300-\u036f]".r
    val multiSpaceRegex  = """\s+""".r
    val badChars         = """[^\w\-]+""".r
    name =>
      badChars.replaceAllIn(
        multiSpaceRegex.replaceAllIn(
          splitAccentRegex.replaceAllIn(
            // split an accented letter in the base letter and the accent
            Normalizer.normalize(name.value, Normalizer.Form.NFD),
            ""
          ),
          "_"
        ),
        ""
      )
