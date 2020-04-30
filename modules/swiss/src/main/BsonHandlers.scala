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
  implicit val swissPointsHandler  = intAnyValHandler[Swiss.Points](_.double, Swiss.Points.apply)
  implicit val swissScoreHandler   = intAnyValHandler[Swiss.Score](_.double, Swiss.Score.apply)
  implicit val playerNumberHandler = intAnyValHandler[SwissPlayer.Number](_.value, SwissPlayer.Number.apply)
  implicit val roundNumberHandler  = intAnyValHandler[SwissRound.Number](_.value, SwissRound.Number.apply)
  implicit val swissIdHandler      = stringAnyValHandler[Swiss.Id](_.value, Swiss.Id.apply)
  implicit val playerIdHandler     = stringAnyValHandler[SwissPlayer.Id](_.value, SwissPlayer.Id.apply)

  implicit val playerHandler = new BSON[SwissPlayer] {
    def reads(r: BSON.Reader) = SwissPlayer(
      _id = r.get[SwissPlayer.Id]("_id"),
      swissId = r.get[Swiss.Id]("s"),
      number = r.get[SwissPlayer.Number]("n"),
      userId = r str "u",
      rating = r int "r",
      provisional = r boolD "pr",
      points = r.get[Swiss.Points]("p"),
      score = r.get[Swiss.Score]("c")
    )
    def writes(w: BSON.Writer, o: SwissPlayer) = $doc(
      "_id" -> o._id,
      "s"   -> o.swissId,
      "n"   -> o.number,
      "u"   -> o.userId,
      "r"   -> o.rating,
      "pr"  -> w.boolO(o.provisional),
      "p"   -> o.points,
      "c"   -> o.score
    )
  }

  implicit val pairingHandler = new BSON[SwissPairing] {
    def reads(r: BSON.Reader) =
      r.get[List[SwissPlayer.Number]]("u") match {
        case List(white, black) =>
          SwissPairing(
            _id = r str "_id",
            swissId = r.get[Swiss.Id]("s"),
            round = r.get[SwissRound.Number]("r"),
            white = white,
            black = black,
            winner = r boolO "w" map {
              case true => white
              case _    => black
            }
          )
        case _ => sys error "Invalid swiss pairing users"
      }
    def writes(w: BSON.Writer, o: SwissPairing) = $doc(
      "_id" -> o._id,
      "s"   -> o.swissId,
      "r"   -> o.round,
      "g"   -> o.gameId,
      "u"   -> o.players,
      "w"   -> o.winner.map(o.white ==)
    )
  }

  implicit val swissHandler = Macros.handler[Swiss]
}
