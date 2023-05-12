package lila.game

import chess.{ Color, Status }

case class LightGame(
    id: GameId,
    whitePlayer: LightPlayer,
    blackPlayer: LightPlayer,
    status: Status,
    win: Option[Color]
):
  def playable                                            = status < Status.Aborted
  def player(color: Color): LightPlayer                   = color.fold(whitePlayer, blackPlayer)
  def players                                             = List(whitePlayer, blackPlayer)
  def playerByUserId(userId: UserId): Option[LightPlayer] = players.find(_.userId contains userId)
  def finished                                            = status >= Status.Mate

object LightGame:

  import Game.{ BSONFields as F }

  def projection =
    lila.db.dsl.$doc(
      F.whitePlayer -> true,
      F.blackPlayer -> true,
      F.playerUids  -> true,
      F.winnerColor -> true,
      F.status      -> true
    )

case class LightPlayer(
    color: Color,
    aiLevel: Option[Int],
    userId: Option[UserId] = None,
    rating: Option[IntRating] = None,
    ratingDiff: Option[IntRatingDiff] = None,
    provisional: RatingProvisional = RatingProvisional.No,
    berserk: Boolean = false
)

object LightPlayer:

  import reactivemongo.api.bson.*
  import lila.db.dsl.{ *, given }

  private[game] type Builder = Color => Option[UserId] => LightPlayer

  private def safeRange[A](range: Range)(a: A)(using ir: IntRuntime[A]): Option[A] =
    range.contains(ir(a)) option a
  private val ratingRange     = safeRange[IntRating](0 to 4000)
  private val ratingDiffRange = safeRange[IntRatingDiff](-1000 to 1000)

  given lightPlayerReader: BSONDocumentReader[Builder] with
    import scala.util.{ Try, Success }
    def readDocument(doc: Bdoc): Try[Builder] = Success(builderRead(doc))

  def builderRead(doc: Bdoc): Builder = color =>
    userId =>
      import Player.BSONFields.*
      LightPlayer(
        color = color,
        aiLevel = doc int aiLevel,
        userId = userId,
        rating = doc.getAsOpt[IntRating](rating) flatMap ratingRange,
        ratingDiff = doc.getAsOpt[IntRatingDiff](ratingDiff) flatMap ratingDiffRange,
        provisional = ~doc.getAsOpt[RatingProvisional](provisional),
        berserk = doc booleanLike berserk getOrElse false
      )
