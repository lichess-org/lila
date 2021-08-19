package lila.game

import shogi.{ CheckCount, Clock, Color, Gote, Hands, Sente }
import Game.BSONFields._
import reactivemongo.api.bson._
import scala.util.Try

import shogi.Centis
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.ByteArray
import lila.db.ByteArray.ByteArrayBSONHandler

object GameDiff {

  private type Set   = (String, BSONValue)
  private type Unset = (String, BSONValue)

  private type ClockHistorySide = (Centis, Vector[Centis], Boolean)

  private type PeriodEntriesSide = Vector[Int]

  type Diff = (List[Set], List[Unset])

  private val w = lila.db.BSON.writer

  def apply(a: Game, b: Game): Diff = {

    val setBuilder   = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A](name: String, getter: Game => A, toBson: A => BSONValue): Unit = {
      val vb = getter(b)
      if (getter(a) != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> bTrue)
        else setBuilder += name                                         -> toBson(vb)
      }
    }

    def dOpt[A](name: String, getter: Game => A, toBson: A => Option[BSONValue]): Unit = {
      val vb = getter(b)
      if (getter(a) != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> bTrue)
        else
          toBson(vb) match {
            case None    => unsetBuilder += (name -> bTrue)
            case Some(x) => setBuilder += name    -> x
          }
      }
    }

    def dTry[A](name: String, getter: Game => A, toBson: A => Try[BSONValue]): Unit =
      d[A](name, getter, a => toBson(a).get)

    def getClockHistory(color: Color)(g: Game): Option[ClockHistorySide] =
      for {
        clk     <- g.clock
        history <- g.clockHistory
        curColor = g.turnColor
        times    = history(color)
      } yield (clk.limit, times, g.flagged has color)

    def clockHistoryToBytes(o: Option[ClockHistorySide]) =
      o.flatMap { case (x, y, z) =>
        ByteArrayBSONHandler.writeOpt(BinaryFormat.clockHistory.writeSide(x, y, z))
      }

    def getPeriodEntries(color: Color)(g: Game): Option[Vector[Int]] =
      for {
        history <- g.clockHistory
      } yield history.periodEntries(color)

    def periodEntriesToBytes(o: Option[PeriodEntriesSide]) =
      o.flatMap { x =>
        ByteArrayBSONHandler.writeOpt(BinaryFormat.periodEntries.writeSide(x))
      }

    // if (false) dTry(huffmanPgn, _.pgnMoves, writeBytes compose PgnStorage.Huffman.encode)
    // else {
    val f = PgnStorage.OldBin
    dTry(oldPgn, _.pgnMoves, writeBytes compose f.encode)
    dTry(binaryPieces, _.board.pieces, writeBytes compose BinaryFormat.piece.write)
    d(positionHashes, _.history.positionHashes, w.bytes)
    d(historyLastMove, _.history.lastMove.map(_.uci) | "", w.str)
    // since variants are always OldBin
    if (a.variant.standard || a.variant.fromPosition)
      dOpt(
        checkCount,
        _.history.checkCount,
        (o: CheckCount) => o.nonEmpty ?? { BSONHandlers.checkCountWriter writeOpt o }
      )
    d(
      crazyData,
      _.board.crazyData.map(_.exportHands) | "",
      w.str
    )
    d(turns, _.turns, w.int)
    dOpt(moveTimes, _.binaryMoveTimes, (o: Option[ByteArray]) => o flatMap ByteArrayBSONHandler.writeOpt)
    dOpt(senteClockHistory, getClockHistory(Sente), clockHistoryToBytes)
    dOpt(goteClockHistory, getClockHistory(Gote), clockHistoryToBytes)
    dOpt(periodsSente, getPeriodEntries(Sente), periodEntriesToBytes)
    dOpt(periodsGote, getPeriodEntries(Gote), periodEntriesToBytes)
    dOpt(
      clock,
      _.clock,
      (o: Option[Clock]) =>
        o flatMap { c =>
          BSONHandlers.clockBSONWrite(a.createdAt, c).toOption
        }
    )
    for (i <- 0 to 1) {
      import Player.BSONFields._
      val name                   = s"p$i."
      val player: Game => Player = if (i == 0) (_.sentePlayer) else (_.gotePlayer)
      dOpt(s"$name$lastDrawOffer", player(_).lastDrawOffer, (l: Option[Int]) => l flatMap w.intO)
      dOpt(s"$name$isOfferingDraw", player(_).isOfferingDraw, w.boolO)
      dOpt(s"$name$proposeTakebackAt", player(_).proposeTakebackAt, w.intO)
      dTry(s"$name$blursBits", player(_).blurs, Blurs.BlursBSONHandler.writeTry)
    }
    dTry(movedAt, _.movedAt, BSONJodaDateTimeHandler.writeTry)

    (setBuilder.toList, unsetBuilder.toList)
  }

  private val bTrue = BSONBoolean(true)

  private val writeBytes = ByteArrayBSONHandler.writeTry _
}
