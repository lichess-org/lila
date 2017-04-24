package lila.game

import chess.{ Color, White, Black, Clock, CheckCount, UnmovedRooks }
import chess.variant.Crazyhouse
import Game.BSONFields._
import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.ByteArray
import lila.db.ByteArray.ByteArrayBSONHandler
import chess.Centis

private[game] object GameDiff {

  type Set = BSONElement // [String, BSONValue]
  type Unset = BSONElement // [String, BSONBoolean]

  type ClockHistorySide = (Centis, Vector[Centis], Boolean)

  def apply(a: Game, b: Game): (List[Set], List[Unset]) = {

    val setBuilder = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A, B <: BSONValue](name: String, getter: Game => A, toBson: A => B) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> bTrue)
        else setBuilder += name -> toBson(vb)
      }
    }

    def dOpt[A, B <: BSONValue](name: String, getter: Game => A, toBson: A => Option[B]) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> bTrue)
        else toBson(vb) match {
          case None => unsetBuilder += (name -> bTrue)
          case Some(x) => setBuilder += name -> x
        }
      }
    }

    def getClockHistory(color: Color)(g: Game): Option[ClockHistorySide] =
      for {
        clk <- g.clock
        history <- g.clockHistory
        curColor = g.turnColor
        times = history(color)
      } yield (clk.limit, times, g.flagged has color)

    def clockHistoryToBytes(o: Option[ClockHistorySide]) = o.map {
      case (x, y, z) => ByteArrayBSONHandler.write(BinaryFormat.clockHistory.writeSide(x, y, z))
    }

    val w = lila.db.BSON.writer

    d(binaryPieces, _.binaryPieces, ByteArrayBSONHandler.write)
    d(binaryPgn, _.binaryPgn, ByteArrayBSONHandler.write)
    d(status, _.status.id, w.int)
    d(turns, _.turns, w.int)
    d(castleLastMoveTime, _.castleLastMoveTime, CastleLastMoveTime.castleLastMoveTimeBSONHandler.write)
    d(unmovedRooks, _.unmovedRooks, (x: UnmovedRooks) => ByteArrayBSONHandler.write(BinaryFormat.unmovedRooks write x))
    dOpt(moveTimes, _.binaryMoveTimes, (o: Option[ByteArray]) => o map ByteArrayBSONHandler.write)
    dOpt(whiteClockHistory, getClockHistory(White), clockHistoryToBytes)
    dOpt(blackClockHistory, getClockHistory(Black), clockHistoryToBytes)
    dOpt(positionHashes, _.positionHashes, w.bytesO)
    dOpt(clock, _.clock, (o: Option[Clock]) => o map { c =>
      BSONHandlers.clockBSONWrite(a.createdAt, c)
    })
    dOpt(checkCount, _.checkCount, (o: CheckCount) => o.nonEmpty option { BSONHandlers.checkCountWriter write o })
    if (a.variant == Crazyhouse)
      dOpt(crazyData, _.crazyData, (o: Option[Crazyhouse.Data]) => o map BSONHandlers.crazyhouseDataBSONHandler.write)
    for (i ← 0 to 1) {
      import Player.BSONFields._
      val name = s"p$i."
      val player: Game => Player = if (i == 0) (_.whitePlayer) else (_.blackPlayer)
      dOpt(s"$name$lastDrawOffer", player(_).lastDrawOffer, w.map[Option, Int, BSONInteger])
      dOpt(s"$name$isOfferingDraw", player(_).isOfferingDraw, w.boolO)
      dOpt(s"$name$isOfferingRematch", player(_).isOfferingRematch, w.boolO)
      dOpt(s"$name$proposeTakebackAt", player(_).proposeTakebackAt, w.intO)
      dOpt(s"$name$blurs", player(_).blurs, w.intO)
    }

    (addUa(setBuilder.toList), unsetBuilder.toList)
  }

  private val bTrue = BSONBoolean(true)

  private def addUa(sets: List[Set]): List[Set] = sets match {
    case Nil => Nil
    case sets => (Game.BSONFields.updatedAt -> BSONJodaDateTimeHandler.write(DateTime.now)) :: sets
  }
}
