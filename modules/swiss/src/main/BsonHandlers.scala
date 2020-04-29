package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import chess.StartingPosition
import lila.db.BSON
import lila.db.dsl._
import reactivemongo.api.bson._

private object BsonHandlers {

  implicit private[swiss] val statusHandler = tryHandler[Status](
    { case BSONInteger(v) => Status(v) toTry s"No such status: $v" },
    x => BSONInteger(x.id)
  )

  implicit val clockHandler = tryHandler[ClockConfig](
    {
      case doc: BSONDocument =>
        for {
          limit <- doc.getAsTry[Int]("limit")
          inc   <- doc.getAsTry[Int]("increment")
        } yield ClockConfig(limit, inc)
    },
    c =>
      BSONDocument(
        "limit"     -> c.limitSeconds,
        "increment" -> c.incrementSeconds
      )
  )
  implicit val variantHandler = lila.db.dsl.quickHandler[Variant](
    {
      case BSONString(v) => Variant orDefault v
      case _             => Variant.default
    },
    v => BSONString(v.key)
  )
  private lazy val fenIndex: Map[String, StartingPosition] = StartingPosition.all.view.map { p =>
    p.fen -> p
  }.toMap
  implicit val startingPositionHandler = lila.db.dsl.quickHandler[StartingPosition](
    {
      case BSONString(v) => fenIndex.getOrElse(v, StartingPosition.initial)
      case _             => StartingPosition.initial
    },
    v => BSONString(v.fen)
  )

  implicit val playerNumberHandler = intAnyValHandler[SwissPlayer.Number](_.value, SwissPlayer.Number.apply)
  implicit val playerIdHandler = tryHandler[SwissPlayer.Id](
    {
      case BSONString(v) =>
        (v split ':' match {
          case Array(swissId, number) =>
            number.toIntOption map { n =>
              SwissPlayer.Id(Swiss.Id(swissId), SwissPlayer.Number(n))
            }
          case _ => None
        }) toTry s"Invalid player ID $v"
    },
    id => BSONString(s"${id.swissId}:${id.number}")
  )

  implicit val playerHandler = new BSON[SwissPlayer] {
    def reads(r: BSON.Reader) = SwissPlayer(
      id = r.get[SwissPlayer.Id]("_id"),
      userId = r str "uid",
      rating = r int "r",
      provisional = r boolD "pr"
    )
    def writes(w: BSON.Writer, o: SwissPlayer) = $doc(
      "_id" -> o.id,
      "uid" -> o.userId,
      "r"   -> o.rating,
      "pr"  -> w.boolO(o.provisional)
    )
  }

  implicit val pairingHandler = new BSON[SwissPairing] {
    def reads(r: BSON.Reader) = {
      val white = r.get[SwissPlayer.Number]("w")
      val black = r.get[SwissPlayer.Number]("b")
      SwissPairing(
        gameId = r str "g",
        white = white,
        black = black,
        winner = r boolO "w" map {
          case true => white
          case _    => black
        }
      )
    }
    def writes(w: BSON.Writer, o: SwissPairing) = $doc(
      "g" -> o.gameId,
      "w" -> o.white,
      "b" -> o.black,
      "w" -> o.winner.map(o.white ==)
    )
  }

  implicit val roundNumberHandler = intAnyValHandler[SwissRound.Number](_.value, SwissRound.Number.apply)
  implicit val roundIdHandler = tryHandler[SwissRound.Id](
    {
      case BSONString(v) =>
        (v split ':' match {
          case Array(swissId, number) =>
            number.toIntOption map { n =>
              SwissRound.Id(Swiss.Id(swissId), SwissRound.Number(n))
            }
          case _ => None
        }) toTry s"Invalid round ID $v"
    },
    id => BSONString(id.toString)
  )

  implicit val roundHandler = new BSON[SwissRound] {
    def reads(r: BSON.Reader) =
      SwissRound(
        id = r.get[SwissRound.Id]("_id"),
        pairings = r.get[List[SwissPairing]]("p"),
        byes = r.get[List[SwissPlayer.Number]]("b")
      )
    def writes(w: BSON.Writer, o: SwissRound) = $doc(
      "id" -> o.id,
      "p"  -> o.pairings,
      "b"  -> o.byes
    )
  }

  implicit val swissIdHandler = stringAnyValHandler[Swiss.Id](_.value, Swiss.Id.apply)
  implicit val swissHandler   = Macros.handler[Swiss]
}
