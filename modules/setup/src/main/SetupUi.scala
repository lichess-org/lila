package lila
package setup

import chess.{ Mode, Speed }
import chess.variant.Variant

import lila.core.i18n.{ Translate, I18nKey as trans }

trait SetupUi:

  type SelectChoice = (String, String, Option[String])

  val clockTimeChoices: List[SelectChoice] = List(
    ("0", "0", none),
    ("0.25", "¼", none),
    ("0.5", "½", none),
    ("0.75", "¾", none)
  ) ::: List(
    "1",
    "1.5",
    "2",
    "3",
    "4",
    "5",
    "6",
    "7",
    "8",
    "9",
    "10",
    "11",
    "12",
    "13",
    "14",
    "15",
    "16",
    "17",
    "18",
    "19",
    "20",
    "25",
    "30",
    "35",
    "40",
    "45",
    "60",
    "75",
    "90",
    "105",
    "120",
    "135",
    "150",
    "165",
    "180"
  ).map: v =>
    (v, v, none)

  val clockIncrementChoices: List[SelectChoice] = {
    (0 to 20).toList ::: List(25, 30, 35, 40, 45, 60, 90, 120, 150, 180)
  }.map { s =>
    (s.toString, s.toString, none)
  }

  val corresDaysChoices: List[SelectChoice] =
    ("1", "One day", none) :: List(2, 3, 5, 7, 10, 14).map { d =>
      (d.toString, s"$d days", none)
    }

  def translatedTimeModeChoices(using Translate) =
    List(
      (TimeMode.RealTime.id.toString, trans.site.realTime.txt(), none),
      (TimeMode.Correspondence.id.toString, trans.site.correspondence.txt(), none),
      (TimeMode.Unlimited.id.toString, trans.site.unlimited.txt(), none)
    )

  def translatedSideChoices(using Translate) =
    List(
      ("black", trans.site.black.txt(), none),
      ("random", trans.site.randomColor.txt(), none),
      ("white", trans.site.white.txt(), none)
    )

  def translatedModeChoices(using Translate) =
    List(
      (Mode.Casual.id.toString, trans.site.casual.txt(), none),
      (Mode.Rated.id.toString, trans.site.rated.txt(), none)
    )

  def translatedIncrementChoices(using Translate) =
    List(
      (1, trans.site.yes.txt(), none),
      (0, trans.site.no.txt(), none)
    )

  def translatedModeChoicesTournament(using Translate) =
    List(
      (Mode.Casual.id.toString, trans.site.casualTournament.txt(), none),
      (Mode.Rated.id.toString, trans.site.ratedTournament.txt(), none)
    )

  private val encodeId = (v: Variant) => v.id.toString

  private def variantTupleId = variantTuple(encodeId)

  private def variantTuple(encode: Variant => String)(variant: Variant) =
    (encode(variant), variant.name, variant.title.some)

  def translatedVariantChoices(using Translate): List[SelectChoice] =
    translatedVariantChoices(encodeId)

  def translatedVariantChoices(encode: Variant => String)(using Translate): List[SelectChoice] =
    List(
      (encode(chess.variant.Standard), trans.site.standard.txt(), chess.variant.Standard.title.some)
    )

  def translatedVariantChoicesWithVariantsById(using Translate): List[SelectChoice] =
    translatedVariantChoicesWithVariants(encodeId)

  def translatedVariantChoicesWithVariants(
      encode: Variant => String
  )(using Translate): List[SelectChoice] =
    translatedVariantChoices(encode) ::: List(
      chess.variant.Crazyhouse,
      chess.variant.Chess960,
      chess.variant.KingOfTheHill,
      chess.variant.ThreeCheck,
      chess.variant.Antichess,
      chess.variant.Atomic,
      chess.variant.Horde,
      chess.variant.RacingKings
    ).map(variantTuple(encode))

  def translatedVariantChoicesWithFen(using Translate) =
    translatedVariantChoices :+
      variantTupleId(chess.variant.Chess960) :+
      variantTupleId(chess.variant.FromPosition)

  def translatedAiVariantChoices(using Translate) =
    translatedVariantChoices :+
      variantTupleId(chess.variant.Crazyhouse) :+
      variantTupleId(chess.variant.Chess960) :+
      variantTupleId(chess.variant.KingOfTheHill) :+
      variantTupleId(chess.variant.ThreeCheck) :+
      variantTupleId(chess.variant.Antichess) :+
      variantTupleId(chess.variant.Atomic) :+
      variantTupleId(chess.variant.Horde) :+
      variantTupleId(chess.variant.RacingKings) :+
      variantTupleId(chess.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(using Translate) =
    translatedVariantChoicesWithVariantsById :+
      variantTupleId(chess.variant.FromPosition)

  def translatedSpeedChoices(using Translate) =
    Speed.limited.map: s =>
      val minutes = s.range.max / 60 + 1
      (
        s.id.toString,
        s.toString + " - " + trans.site.lessThanNbMinutes.pluralSameTxt(minutes),
        none
      )
