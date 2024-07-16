package lila.game

import chess.{ Black, Centis, CheckCount, Clock, Color, White }
import reactivemongo.api.bson.*

import scala.util.Try

import lila.core.game.{ Game, Player }
import lila.db.ByteArray
import lila.db.ByteArray.given
import lila.db.dsl.given
import lila.game.Game.BSONFields.*

object GameDiff:

  private type Set   = (String, BSONValue)
  private type Unset = (String, BSONValue)

  private type ClockHistorySide = (Centis, Vector[Centis], Boolean)

  type Diff = (List[Set], List[Unset])

  private val w = lila.db.BSON.writer

  def apply(a: Game, b: Game): Diff =

    val setBuilder   = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A](name: String, getter: Game => A, toBson: A => BSONValue): Unit =
      val vb = getter(b)
      if getter(a) != vb then
        if vb == None || vb == null || vb == "" then unsetBuilder += (name -> bTrue)
        else setBuilder += name -> toBson(vb)

    def dOpt[A](name: String, getter: Game => A, toBson: A => Option[BSONValue]): Unit =
      val vb = getter(b)
      if getter(a) != vb then
        if vb == None || vb == null || vb == "" then unsetBuilder += (name -> bTrue)
        else
          toBson(vb) match
            case None    => unsetBuilder += (name -> bTrue)
            case Some(x) => setBuilder += name -> x

    def dTry[A](name: String, getter: Game => A, toBson: A => Try[BSONValue]): Unit =
      d[A](name, getter, a => toBson(a).get)

    def getClockHistory(color: Color)(g: Game): Option[ClockHistorySide] =
      for
        clk     <- g.clock
        history <- g.clockHistory
        curColor = g.turnColor
        times    = history(color)
      yield (clk.limit, times, g.flagged.has(color))

    def clockHistoryToBytes(o: Option[ClockHistorySide]) =
      o.flatMap { case (x, y, z) =>
        byteArrayHandler.writeOpt(BinaryFormat.clockHistory.writeSide(x, y, z))
      }

    if a.variant.standard then dTry(huffmanPgn, _.sans, writeBytes.compose(PgnStorage.Huffman.encode))
    else
      val f = PgnStorage.OldBin
      dTry(oldPgn, _.sans, writeBytes.compose(f.encode))
      dTry(binaryPieces, _.board.pieces, writeBytes.compose(BinaryFormat.piece.write))
      d(positionHashes, _.history.positionHashes, ph => w.bytes(ph.value))
      dTry(unmovedRooks, _.history.unmovedRooks, writeBytes.compose(BinaryFormat.unmovedRooks.write))
      dTry(castleLastMove, makeCastleLastMove, CastleLastMove.castleLastMoveHandler.writeTry)
      // since variants are always OldBin
      if a.variant.threeCheck then
        dOpt(
          checkCount,
          _.history.checkCount,
          (o: CheckCount) => o.nonEmpty.so { BSONHandlers.checkCountWriter.writeOpt(o) }
        )
      if a.variant.crazyhouse then
        dOpt(
          crazyData,
          _.board.crazyData,
          (o: Option[chess.variant.Crazyhouse.Data]) => o.map(BSONHandlers.crazyhouseDataHandler.write)
        )
    d(turns, _.ply, ply => w.int(ply.value))
    dOpt(moveTimes, _.binaryMoveTimes, (o: Option[Array[Byte]]) => o.flatMap(arrayByteHandler.writeOpt))
    dOpt(whiteClockHistory, getClockHistory(White), clockHistoryToBytes)
    dOpt(blackClockHistory, getClockHistory(Black), clockHistoryToBytes)
    dOpt(
      clock,
      _.clock,
      (o: Option[Clock]) =>
        o.flatMap { c =>
          BSONHandlers.clockBSONWrite(a.createdAt, c).toOption
        }
    )
    dTry(drawOffers, _.drawOffers, BSONHandlers.gameDrawOffersHandler.writeTry)
    for i <- 0 to 1 do
      import lila.game.Player.BSONFields.*
      val name                   = s"p$i."
      val player: Game => Player = if i == 0 then (_.whitePlayer) else (_.blackPlayer)
      dOpt(s"$name$isOfferingDraw", player(_).isOfferingDraw, w.boolO)
      dOpt(s"$name$proposeTakebackAt", player(_).proposeTakebackAt, ply => w.intO(ply.value))
      dTry(s"$name$blursBits", player(_).blurs, Blurs.blursHandler.writeTry)
    dTry(movedAt, _.movedAt, instantHandler.writeTry)

    (setBuilder.toList, unsetBuilder.toList)

  private val bTrue = BSONBoolean(true)

  private val writeBytes = byteArrayHandler.writeTry

  private def makeCastleLastMove(g: Game) =
    CastleLastMove(
      lastMove = g.history.lastMove,
      castles = g.history.castles
    )
