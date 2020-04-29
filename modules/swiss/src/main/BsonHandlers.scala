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
  implicit val swissPointsHandler = intAnyValHandler[Swiss.Points](_.double, Swiss.Points.apply)

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
      provisional = r boolD "pr",
      points = r.get[Swiss.Points]("p")
    )
    def writes(w: BSON.Writer, o: SwissPlayer) = $doc(
      "_id" -> o.id,
      "uid" -> o.userId,
      "r"   -> o.rating,
      "pr"  -> w.boolO(o.provisional),
      "p"   -> o.points
    )
  }

  implicit val swissIdHandler     = stringAnyValHandler[Swiss.Id](_.value, Swiss.Id.apply)
  implicit val pairingIdHandler   = stringAnyValHandler[SwissPairing.Id](_.value, SwissPairing.Id.apply)
  implicit val roundNumberHandler = intAnyValHandler[SwissRound.Number](_.value, SwissRound.Number.apply)

  implicit val pairingHandler = new BSON[SwissPairing] {
    def reads(r: BSON.Reader) = {
      val white = r.get[SwissPlayer.Number]("w")
      val black = r.get[SwissPlayer.Number]("b")
      SwissPairing(
        _id = r.get[SwissPairing.Id]("_id"),
        swissId = r.get[Swiss.Id]("s"),
        round = r.get[SwissRound.Number]("r"),
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
      "_id" -> o._id,
      "s"   -> o.swissId,
      "r"   -> o.round,
      "g"   -> o.gameId,
      "w"   -> o.white,
      "b"   -> o.black,
      "w"   -> o.winner.map(o.white ==)
    )
  }

  implicit val swissHandler = Macros.handler[Swiss]
}
