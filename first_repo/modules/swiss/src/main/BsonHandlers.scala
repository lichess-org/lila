package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.Color
import chess.format.FEN
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.db.BSON
import lila.db.dsl._
import lila.user.User

private object BsonHandlers {

  implicit val variantHandler       = variantByKeyHandler
  implicit val clockHandler         = clockConfigHandler
  implicit val swissPointsHandler   = intAnyValHandler[Swiss.Points](_.double, Swiss.Points.apply)
  implicit val swissTieBreakHandler = doubleAnyValHandler[Swiss.TieBreak](_.value, Swiss.TieBreak.apply)
  implicit val swissPerformanceHandler =
    floatAnyValHandler[Swiss.Performance](_.value, Swiss.Performance.apply)
  implicit val swissScoreHandler  = intAnyValHandler[Swiss.Score](_.value, Swiss.Score.apply)
  implicit val roundNumberHandler = intAnyValHandler[SwissRound.Number](_.value, SwissRound.Number.apply)
  implicit val swissIdHandler     = stringAnyValHandler[Swiss.Id](_.value, Swiss.Id.apply)
  implicit val playerIdHandler    = stringAnyValHandler[SwissPlayer.Id](_.value, SwissPlayer.Id.apply)

  implicit val playerHandler = new BSON[SwissPlayer] {
    import SwissPlayer.Fields._
    def reads(r: BSON.Reader) =
      SwissPlayer(
        id = r.get[SwissPlayer.Id](id),
        swissId = r.get[Swiss.Id](swissId),
        userId = r str userId,
        rating = r int rating,
        provisional = r boolD provisional,
        points = r.get[Swiss.Points](points),
        tieBreak = r.get[Swiss.TieBreak](tieBreak),
        performance = r.getO[Swiss.Performance](performance),
        score = r.get[Swiss.Score](score),
        absent = r.boolD(absent),
        byes = ~r.getO[Set[SwissRound.Number]](byes)
      )
    def writes(w: BSON.Writer, o: SwissPlayer) =
      $doc(
        id          -> o.id,
        swissId     -> o.swissId,
        userId      -> o.userId,
        rating      -> o.rating,
        provisional -> w.boolO(o.provisional),
        points      -> o.points,
        tieBreak    -> o.tieBreak,
        performance -> o.performance,
        score       -> o.score,
        absent      -> w.boolO(o.absent),
        byes        -> o.byes.some.filter(_.nonEmpty)
      )
  }

  implicit val pairingStatusHandler = lila.db.dsl.quickHandler[SwissPairing.Status](
    {
      case BSONBoolean(true)  => Left(SwissPairing.Ongoing)
      case BSONInteger(index) => Right(Color.fromWhite(index == 0).some)
      case _                  => Right(none)
    },
    {
      case Left(_)        => BSONBoolean(true)
      case Right(Some(c)) => BSONInteger(c.fold(0, 1))
      case _              => BSONNull
    }
  )
  implicit val pairingHandler = new BSON[SwissPairing] {
    import SwissPairing.Fields._
    def reads(r: BSON.Reader) =
      r.get[List[User.ID]](players) match {
        case List(w, b) =>
          SwissPairing(
            id = r str id,
            swissId = r.get[Swiss.Id](swissId),
            round = r.get[SwissRound.Number](round),
            white = w,
            black = b,
            status = r.getO[SwissPairing.Status](status) | Right(none)
          )
        case _ => sys error "Invalid swiss pairing users"
      }
    def writes(w: BSON.Writer, o: SwissPairing) =
      $doc(
        id      -> o.id,
        swissId -> o.swissId,
        round   -> o.round,
        players -> o.players,
        status  -> o.status
      )
  }

  import SwissCondition.BSONHandlers.AllBSONHandler

  implicit val settingsHandler = new BSON[Swiss.Settings] {
    def reads(r: BSON.Reader) =
      Swiss.Settings(
        nbRounds = r.get[Int]("n"),
        rated = r.boolO("r") | true,
        description = r.strO("d"),
        position = r.getO[FEN]("f"),
        chatFor = r.intO("c") | Swiss.ChatFor.default,
        roundInterval = (r.intO("i") | 60).seconds,
        password = r.strO("p"),
        conditions = r.getO[SwissCondition.All]("o") getOrElse SwissCondition.All.empty,
        forbiddenPairings = r.getD[String]("fp")
      )
    def writes(w: BSON.Writer, s: Swiss.Settings) =
      $doc(
        "n" -> s.nbRounds,
        "r" -> (!s.rated).option(false),
        "d" -> s.description,
        "f" -> s.position,
        "c" -> (s.chatFor != Swiss.ChatFor.default).option(s.chatFor),
        "i" -> s.roundInterval.toSeconds.toInt,
        "p" -> s.password,
        "o" -> s.conditions.ifNonEmpty,
        "fp" -> s.forbiddenPairings.some.filter(_.nonEmpty)
      )
  }

  implicit val swissHandler = Macros.handler[Swiss]

  def addFeaturable(s: Swiss) =
    swissHandler.writeTry(s).get ++ {
      s.isNotFinished ?? $doc(
        "featurable" -> true,
        "garbage"    -> s.unrealisticSettings.option(true)
      )
    }
}
