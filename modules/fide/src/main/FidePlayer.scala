package lila.fide

import reactivemongo.api.bson.Macros.Annotations.Key
import java.text.Normalizer
import chess.{ FideId, PlayerTitle, PlayerName }

case class FidePlayer(
    @Key("_id") id: FideId,
    name: PlayerName,
    token: PlayerToken,
    fed: Option[Federation.Id],
    title: Option[PlayerTitle],
    standard: Option[Int],
    rapid: Option[Int],
    blitz: Option[Int],
    year: Option[Int],
    inactive: Option[Boolean],
    fetchedAt: Instant
):
  def ratingOf(tc: FideTC): Option[Int] = tc match
    case FideTC.standard => standard
    case FideTC.rapid    => rapid
    case FideTC.blitz    => blitz

  def slug: String = FidePlayer.nameToSlug(name)

  def age: Option[Int] = year.map(nowInstant.date.getYear - _)

object FidePlayer:
  private val nonLetterRegex = """[^a-zA-Z0-9\s]+""".r
  private val splitRegex     = """\W""".r
  def tokenize(str: String): PlayerToken =
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

  object nameToSlug:
    private val splitAccentRegex = "[\u0300-\u036f]".r
    private val multiSpaceRegex  = """\s+""".r
    private val badChars         = """[^\w\-]+""".r
    def apply(name: PlayerName): String =
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
