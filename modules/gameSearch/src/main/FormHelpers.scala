package lila.gameSearch

import chess.Status

import lila.core.rating.RatingRange

object FormHelpers:

  import lila.common.Form.*
  import lila.core.i18n.{ Translate, I18nKey as trans }

  def durations(using Translate): List[(Int, String)] =
    ((30, trans.site.nbSeconds.pluralSameTxt(30)) ::
      options(
        List(60, 60 * 2, 60 * 3, 60 * 5, 60 * 10, 60 * 15, 60 * 20, 60 * 30),
        i => trans.site.nbMinutes.pluralSameTxt(i / 60)
      ).toList) :+
      (60 * 60 * 1 -> trans.site.nbHours.pluralSameTxt(1)) :+
      (60 * 60 * 2 -> trans.site.nbHours.pluralSameTxt(2)) :+
      (60 * 60 * 3 -> trans.site.nbHours.pluralSameTxt(3))

  def clockInits(using Translate) = List(
    (30, trans.site.nbSeconds.pluralSameTxt(30)),
    (45, trans.site.nbSeconds.pluralSameTxt(45))
  ) ::: options(
    List(
      60 * 1,
      60 * 2,
      60 * 3,
      60 * 5,
      60 * 10,
      60 * 15,
      60 * 20,
      60 * 30,
      60 * 45,
      60 * 60,
      60 * 90,
      60 * 120,
      60 * 150,
      60 * 180
    ),
    i => trans.site.nbMinutes.pluralSameTxt(i / 60)
  ).toList

  def clockIncs(using Translate) =
    options(
      List(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 150, 180),
      i => trans.site.nbSeconds.pluralSameTxt(i)
    ).toList

  def winnerColors(using Translate) = List(1 -> trans.site.white.txt(), 2 -> trans.site.black.txt())

  val sources = lila.core.game.Source.searchable.map { v =>
    v.id -> v.name.capitalize
  }

  def modes(using Translate) = List(0 -> trans.site.casual.txt(), 1 -> trans.site.rated.txt())

  val turns = options(
    (1 to 5) ++ (10 to 45 by 5) ++ (50 to 90 by 10) ++ (100 to 300 by 25),
    _.toString
  )

  val averageRatings = (RatingRange.min.value to RatingRange.max.value by 100).toList.map { e =>
    e -> e.toString
  }

  def hasAis(using Translate) = List(0 -> trans.site.human.txt(), 1 -> trans.site.computer.txt())

  val aiLevels = (1 to 8).map { l =>
    l -> s"level $l"
  }

  val dates = List("0d" -> "Now") ++
    options(List(1, 2, 6), "h", "%d hour{s} ago") ++
    options(1 to 6, "d", "%d day{s} ago") ++
    options(1 to 3, "w", "%d week{s} ago") ++
    options(1 to 6, "m", "%d month{s} ago") ++
    options(1 to 5, "y", "%d year{s} ago")

  val statuses = Status.finishedNotCheated.flatMap:
    case s if s.is(_.Timeout)       => none
    case s if s.is(_.NoStart)       => none
    case s if s.is(_.UnknownFinish) => none
    case s if s.is(_.Outoftime)     => Some(s.id -> "Clock Flag")
    case s if s.is(_.VariantEnd)    => Some(s.id -> "Variant End")
    case s                          => Some(s.id -> s.toString)
