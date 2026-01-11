package lila.fide

import chess.{ FideId, FideTC, PlayerName, PlayerTitle }
import chess.rating.{ Elo, KFactor }
import reactivemongo.api.bson.Macros.Annotations.Key

import java.text.Normalizer

import lila.core.fide.{ PlayerToken, Tokenize, diacritics }
import lila.core.id.ImageId

case class FidePlayer(
    @Key("_id") id: FideId,
    name: PlayerName,
    token: PlayerToken,
    photo: Option[FidePlayer.PlayerPhoto],
    fed: Option[lila.core.fide.Federation.Id],
    title: Option[PlayerTitle],
    standard: Option[Elo],
    standardK: Option[KFactor],
    rapid: Option[Elo],
    rapidK: Option[KFactor],
    blitz: Option[Elo],
    blitzK: Option[KFactor],
    year: Option[Int],
    deceasedYear: Option[Int] = None,
    inactive: Boolean
) extends lila.core.fide.Player:

  def ratingOf(tc: FideTC): Option[Elo] = tc match
    case FideTC.standard => standard
    case FideTC.rapid => rapid
    case FideTC.blitz => blitz

  def kFactorOf(tc: FideTC): KFactor = tc
    .match
      case FideTC.standard => standardK
      case FideTC.rapid => rapidK
      case FideTC.blitz => blitzK
    .|(KFactor.default)

  def slug: String = FidePlayer.slugify(name)

  def age: Option[Int] =
    val nowYear = nowInstant.date.getYear
    year.map: birthYear =>
      deceasedYear.fold(nowYear - birthYear)(deceasedYear => deceasedYear - birthYear)

  def ratingsMap: Map[FideTC, Elo] = FideTC.values.flatMap(tc => ratingOf(tc).map(tc -> _)).toMap

  def isSame(other: FidePlayer) = fideData == other.fideData

  private def fideData =
    (name, fed, title, standard, standardK, rapid, rapidK, blitz, blitzK, year, inactive)

  def ratingsStr = List(
    "Standard" -> standard,
    "Rapid" -> rapid,
    "Blitz" -> blitz
  ).map: (name, rating) =>
    s"$name: ${rating.fold("—")(_.toString)}"
  .mkString(", ")

object FidePlayer:

  case class PlayerPhoto(id: ImageId, credit: Option[String] = None)

  object PlayerPhoto:
    enum Size(val width: Int):
      def height = width
      def dimensions = lila.memo.Dimensions(width, height)
      case Medium extends Size(500)
      case Small extends Size(100)
    type SizeSelector = Size.type => Size

    def apply(picfitUrl: lila.memo.PicfitUrl, image: ImageId, size: SizeSelector): Url =
      picfitUrl.thumbnail(image)(size(Size).dimensions)

  object form:
    import play.api.data.*
    import play.api.data.Forms.*
    def credit(p: FidePlayer) =
      Form(single("photo.credit" -> optional(nonEmptyText))).fill(p.photo.flatMap(_.credit))

  case class WithFollow(player: FidePlayer, follow: Boolean)

  private[fide] val tokenize: Tokenize =
    val nonLetterRegex = """[^a-zA-Z0-9\s]+""".r
    val splitRegex = """\W""".r
    str =>
      splitRegex
        .split:
          Normalizer
            .normalize(diacritics.remove(str.trim), Normalizer.Form.NFD)
            .replaceAllIn(nonLetterRegex, "")
            .toLowerCase
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .pipe(trimTitle)
        .distinct
        .sorted
        .mkString(" ")

  private def trimTitle(name: List[String]): List[String] = name match
    case title :: rest if PlayerTitle.get(title).isDefined => rest
    case _ => name

  private[fide] val slugify: PlayerName => String =
    val splitAccentRegex = "[\u0300-\u036f]".r
    val multiSpaceRegex = """\s+""".r
    val badChars = """[^\w\-]+""".r
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
