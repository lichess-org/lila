package lidraughts.game

import draughts.{ Color, White, Black, Clock, KingMoves }
import Game.BSONFields._
import reactivemongo.bson._

import Blurs.BlursBSONWriter
import draughts.Centis
import lidraughts.db.BSON.BSONJodaDateTimeHandler
import lidraughts.db.ByteArray
import lidraughts.db.ByteArray.ByteArrayBSONHandler

object GameDiff {

  private type Set = BSONElement // [String, BSONValue]
  private type Unset = BSONElement // [String, BSONBoolean]

  private type ClockHistorySide = (Centis, Vector[Centis], Boolean)

  type Diff = (List[Set], List[Unset])

  private val w = lidraughts.db.BSON.writer

  def apply(a: Game, b: Game): Diff = {

    val setBuilder = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A, B <: BSONValue](name: String, getter: Game => A, toBson: A => B): Unit = {
      val vb = getter(b)
      if (getter(a) != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> bTrue)
        else setBuilder += name -> toBson(vb)
      }
    }

    def dOpt[A, B <: BSONValue](name: String, getter: Game => A, toBson: A => Option[B]): Unit = {
      val vb = getter(b)
      if (getter(a) != vb) {
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

    a.pdnStorage match {
      case f @ PdnStorage.OldBin => {
        d(oldPdn, _.pdnMoves, writeBytes compose f.encode)
        d(binaryPieces, _.board.pieces, writeBytes compose { m: draughts.PieceMap => BinaryFormat.piece.write(m, a.variant) })
        d(positionHashes, _.history.positionHashes, w.bytes)
        d(historyLastMove, _.history.lastMove.map(_.uci) | "", w.str)
        // since variants are always OldBin
        if (a.variant.frisianVariant || a.variant.russian)
          dOpt(kingMoves, _.history.kingMoves, (o: KingMoves) => o.nonEmpty option { BSONHandlers.kingMovesWriter write o })
      }
      case f @ PdnStorage.Huffman => {
        d(huffmanPdn, _.pdnMoves, writeBytes compose f.encode)
      }
    }

    d(turns, _.displayTurns, w.int)

    dOpt(moveTimes, _.binaryMoveTimes, (o: Option[ByteArray]) => o map ByteArrayBSONHandler.write)
    dOpt(whiteClockHistory, getClockHistory(White), clockHistoryToBytes)
    dOpt(blackClockHistory, getClockHistory(Black), clockHistoryToBytes)
    dOpt(clock, _.clock, (o: Option[Clock]) => o map { c =>
      BSONHandlers.clockBSONWrite(a.createdAt, c)
    })

    for (i ← 0 to 1) {
      import Player.BSONFields._
      val name = s"p$i."
      val player: Game => Player = if (i == 0) (_.whitePlayer) else (_.blackPlayer)
      dOpt(s"$name$lastDrawOffer", player(_).lastDrawOffer, w.map[Option, Int, BSONInteger])
      dOpt(s"$name$isOfferingDraw", player(_).isOfferingDraw, w.boolO)
      dOpt(s"$name$proposeTakebackAt", player(_).proposeTakebackAt, w.intO)
      d(s"$name$blursBits", player(_).blurs, BlursBSONWriter.write)
    }

    d(movedAt, _.movedAt, BSONJodaDateTimeHandler.write)

    (setBuilder.toList, unsetBuilder.toList)

  }

  private val bTrue = BSONBoolean(true)

  private val writeBytes = ByteArrayBSONHandler.write _

}
