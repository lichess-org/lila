package lila.swiss

import chess.format.Fen
import chess.IntRating
import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

object BsonHandlers:

  given BSONHandler[chess.variant.Variant] = variantByKeyHandler
  given BSONHandler[chess.Clock.Config] = clockConfigHandler
  given BSONHandler[SwissPoints] = intAnyValHandler(_.doubled, SwissPoints.fromDoubled)

  given BSON[SwissPlayer] with
    import SwissPlayer.Fields.*
    def reads(r: BSON.Reader) =
      SwissPlayer(
        id = r.get[SwissPlayer.Id](id),
        swissId = r.get(swissId),
        userId = r.get(userId),
        rating = r.get[IntRating](rating),
        provisional = r.yesnoD(provisional),
        points = r.get[SwissPoints](points),
        tieBreak = r.get[Swiss.TieBreak](tieBreak),
        performance = r.getO[Swiss.Performance](performance),
        score = r.get[Swiss.Score](score),
        absent = r.boolD(absent),
        byes = ~r.getO[Set[SwissRoundNumber]](byes)
      )
    def writes(w: BSON.Writer, o: SwissPlayer) =
      $doc(
        id -> o.id,
        swissId -> o.swissId,
        userId -> o.userId,
        rating -> o.rating,
        provisional -> w.yesnoO(o.provisional),
        points -> o.points,
        tieBreak -> o.tieBreak,
        performance -> o.performance,
        score -> o.score,
        absent -> w.boolO(o.absent),
        byes -> o.byes.nonEmptyOption
      )

  /* true = ongoing
   * 0 = white won
   * 1 = black won
   * null = draw
   */
  given BSONHandler[SwissPairing.Status] = lila.db.dsl.quickHandler(
    {
      case BSONBoolean(true) => Left(SwissPairing.Ongoing)
      case BSONInteger(index) => Right(Color.fromWhite(index == 0).some)
      case _ => Right(none)
    },
    {
      case Left(_) => BSONBoolean(true)
      case Right(Some(c)) => BSONInteger(c.fold(0, 1))
      case _ => BSONNull
    }
  )
  given BSON[SwissPairing] with
    import SwissPairing.Fields.*
    def reads(r: BSON.Reader) =
      r.get[List[UserId]](players) match
        case List(w, b) =>
          SwissPairing(
            id = r.get[GameId](id),
            swissId = r.get[SwissId](swissId),
            round = r.get[SwissRoundNumber](round),
            white = w,
            black = b,
            status = r.getO[SwissPairing.Status](status) | Right(none),
            isForfeit = r.boolD(isForfeit)
          )
        case _ => sys.error("Invalid swiss pairing users")
    def writes(w: BSON.Writer, o: SwissPairing) =
      $doc(
        id -> o.id,
        swissId -> o.swissId,
        round -> o.round,
        players -> o.players,
        status -> o.status,
        isForfeit -> w.boolO(o.isForfeit)
      )

  import SwissCondition.bsonHandler

  given BSON[Swiss.Settings] with
    def reads(r: BSON.Reader) =
      Swiss.Settings(
        nbRounds = r.get[Int]("n"),
        rated = chess.Rated(r.boolO("r") | true),
        description = r.strO("d"),
        position = r.getO[Fen.Full]("f"),
        chatFor = r.intO("c") | Swiss.ChatFor.default,
        roundInterval = (r.intO("i") | 60).seconds,
        password = r.strO("p"),
        conditions = r.getD[SwissCondition.All]("o"),
        forbiddenPairings = r.getD[String]("fp"),
        manualPairings = r.getD[String]("mp")
      )
    def writes(w: BSON.Writer, s: Swiss.Settings) =
      $doc(
        "n" -> s.nbRounds,
        "r" -> s.rated.no.option(false),
        "d" -> s.description,
        "f" -> s.position,
        "c" -> (s.chatFor != Swiss.ChatFor.default).option(s.chatFor),
        "i" -> s.roundInterval.toSeconds.toInt,
        "p" -> s.password,
        "o" -> s.conditions,
        "fp" -> s.forbiddenPairings.nonEmptyOption,
        "mp" -> s.manualPairings.nonEmptyOption
      )

  given BSONDocumentHandler[Swiss] = Macros.handler

  // "featurable" mostly means that the tournament isn't over yet
  def addFeaturable(s: Swiss): Bdoc =
    bsonWriteObjTry[Swiss](s).get ++ {
      s.isNotFinished.so(
        $doc(
          "featurable" -> true,
          "garbage" -> s.unrealisticSettings.option(true)
        )
      )
    }

  given BSONDocumentHandler[lila.core.swiss.IdName] = Macros.handler
  given BSONDocumentHandler[SwissBan] = Macros.handler
