package lila.setup
package ui

import play.api.data.{ Form, Field }
import chess.{ Mode, Speed }
import chess.variant.Variant

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.core.rating.RatingRange

final class SetupUi(helpers: Helpers):
  import helpers.{ *, given }

  def setupCheckboxes(
      field: Field,
      options: Seq[(Any, String, Option[String])],
      checks: Set[String] = Set.empty
  ): Frag =
    options.mapWithIndex { case ((value, text, hint), index) =>
      div(cls := "checkable")(
        label(title := hint)(
          input(
            tpe      := "checkbox",
            cls      := "regular-checkbox",
            name     := s"${field.name}[$index]",
            st.value := value.toString,
            checks(value.toString).option(checked)
          ),
          raw(text)
        )
      )
    }

  def filter(form: Form[?])(using ctx: Context) = frag(
    st.form(novalidate)(
      table(
        tbody(
          tr(cls := "variant")(
            td(trans.site.variant()),
            td(
              setupCheckboxes(
                form("variant"),
                translatedVariantChoicesWithVariants(_.key)
              )
            )
          ),
          tr(
            td(trans.site.timeControl()),
            td(setupCheckboxes(form("speed"), translatedSpeedChoices))
          ),
          tr(cls := "inline")(
            td(trans.site.increment()),
            td(
              setupCheckboxes(
                form("increment"),
                translatedIncrementChoices
              )
            )
          ),
          ctx.isAuth.option(
            tr(cls := "inline")(
              td(trans.site.mode()),
              td(setupCheckboxes(form("mode"), translatedModeChoices))
            )
          ),
          ctx.isAuth.option(
            tr(
              td(trans.site.ratingRange()),
              td(
                label(cls := "range")("? - ?"),
                div(cls := "rating-range") {
                  val field = form("ratingRange")
                  frag(
                    form3.hidden(field),
                    input(
                      name := s"${field.name}_range_min",
                      tpe  := "range",
                      cls  := "range rating-range__min",
                      min  := RatingRange.min,
                      max  := RatingRange.max
                    ),
                    "/",
                    input(
                      name := s"${field.name}_range_max",
                      tpe  := "range",
                      cls  := "range rating-range__max",
                      min  := RatingRange.min,
                      max  := RatingRange.max
                    )
                  )
                }
              )
            )
          )
        )
      ),
      div(cls := "actions")(
        button(
          tpe      := "reset",
          cls      := "button button-empty button-red text reset",
          dataIcon := Icon.NotAllowed
        )(
          trans.site.reset()
        ),
        submitButton(cls := "button button-green text apply", dataIcon := Icon.Checkmark)(
          trans.site.apply()
        )
      )
    )
  )

  private type SelectChoice = (String, String, Option[String])

  private val clockTimeChoices: List[SelectChoice] = List(
    ("0", "0", none),
    ("0.25", "¼", none),
    ("0.5", "½", none),
    ("0.75", "¾", none)
  ) ::: List(
    // format: off
    "1", "1.5", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18",
    "19", "20", "25", "30", "35", "40", "45", "60", "75", "90", "105", "120", "135", "150", "165", "180"
  ).map: v =>
    (v, v, none)

  private val clockIncrementChoices: List[SelectChoice] = {
    (0 to 20).toList ::: List(25, 30, 35, 40, 45, 60, 90, 120, 150, 180)
  }.map { s =>
    (s.toString, s.toString, none)
  }

  private val corresDaysChoices: List[SelectChoice] =
    ("1", "One day", none) :: List(2, 3, 5, 7, 10, 14).map { d =>
      (d.toString, s"$d days", none)
    }

  private def translatedTimeModeChoices(using Translate) =
    List(
      (TimeMode.RealTime.id.toString, trans.site.realTime.txt(), none),
      (TimeMode.Correspondence.id.toString, trans.site.correspondence.txt(), none),
      (TimeMode.Unlimited.id.toString, trans.site.unlimited.txt(), none)
    )

  private def translatedSideChoices(using Translate) =
    List(
      ("black", trans.site.black.txt(), none),
      ("random", trans.site.randomColor.txt(), none),
      ("white", trans.site.white.txt(), none)
    )

  private def translatedModeChoices(using Translate) =
    List(
      (Mode.Casual.id.toString, trans.site.casual.txt(), none),
      (Mode.Rated.id.toString, trans.site.rated.txt(), none)
    )

  private def translatedIncrementChoices(using Translate) =
    List(
      (1, trans.site.yes.txt(), none),
      (0, trans.site.no.txt(), none)
    )

  private def translatedModeChoicesTournament(using Translate) =
    List(
      (Mode.Casual.id.toString, trans.site.casualTournament.txt(), none),
      (Mode.Rated.id.toString, trans.site.ratedTournament.txt(), none)
    )

  private def encodeId(v: Variant) = v.id.toString

  private def variantTupleId = variantTuple(encodeId)

  private def variantTuple(encode: Variant => String)(variant: Variant) =
    (encode(variant), variant.name, variant.title.some)

  private def translatedVariantChoices(using Translate): List[SelectChoice] =
    translatedVariantChoices(encodeId)

  private def translatedVariantChoices(encode: Variant => String)(using Translate): List[SelectChoice] =
    List(
      (encode(chess.variant.Standard), trans.site.standard.txt(), chess.variant.Standard.title.some)
    )

  def translatedVariantChoicesWithVariantsById(using Translate): List[SelectChoice] =
    translatedVariantChoicesWithVariants(encodeId)

  def translatedVariantChoicesWithVariants(encode: Variant => String)(using Translate): List[SelectChoice] =
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

  private def translatedVariantChoicesWithFen(using Translate) =
    translatedVariantChoices :+
      variantTupleId(chess.variant.Chess960) :+
      variantTupleId(chess.variant.FromPosition)

  private def translatedAiVariantChoices(using Translate) =
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

  private def translatedVariantChoicesWithVariantsAndFen(using Translate) =
    translatedVariantChoicesWithVariantsById :+
      variantTupleId(chess.variant.FromPosition)

  private def translatedSpeedChoices(using Translate) =
    Speed.limited.map: s =>
      val minutes = s.range.max / 60 + 1
      (
        s.id.toString,
        s.toString + " - " + trans.site.lessThanNbMinutes.pluralSameTxt(minutes),
        none
      )
