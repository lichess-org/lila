package lila.game

import chess.{ Clock, Pos }
import Game.BSONFields._
import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.ByteArray
import lila.db.BSON.BSONJodaDateTimeHandler

private[game] object GameDiff {

  type Set = (String, BSONValue)
  type Unset = (String, BSONBoolean)

  def apply(a: Game, b: Game): (List[Set], List[Unset]) = {

    val setBuilder = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A, B <: BSONValue](name: String, getter: Game => A, toBson: A => B) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> BSONBoolean(true))
        else setBuilder += name -> toBson(vb)
      }
    }

    def dOpt[A, B <: BSONValue](name: String, getter: Game => A, toBson: A => Option[B]) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> BSONBoolean(true))
        else toBson(vb) match {
          case None    => unsetBuilder += (name -> BSONBoolean(true))
          case Some(x) => setBuilder += name -> x
        }
      }
    }

    val w = lila.db.BSON.writer

    d(binaryPieces, _.binaryPieces, ByteArray.ByteArrayBSONHandler.write)
    d(binaryPgn, _.binaryPgn, ByteArray.ByteArrayBSONHandler.write)
    d(status, _.status.id, w.int)
    d(turns, _.turns, w.int)
    d(castleLastMoveTime, _.castleLastMoveTime, CastleLastMoveTime.castleLastMoveTimeBSONHandler.write)
    d(moveTimes, _.moveTimes, (x: Vector[Int]) => ByteArray.ByteArrayBSONHandler.write(BinaryFormat.moveTime write x))
    dOpt(positionHashes, _.positionHashes, w.bytesO)
    dOpt(clock, _.clock, (o: Option[Clock]) => o map { c => Game.clockBSONHandler.write(_ => c) })
    for (i ← 0 to 1) {
      import Player.BSONFields._
      val name = s"p$i."
      val player: Game => Player = if (i == 0) (_.whitePlayer) else (_.blackPlayer)
      dOpt(name + lastDrawOffer, player(_).lastDrawOffer, w.map[Option, Int, BSONInteger])
      dOpt(name + isOfferingDraw, player(_).isOfferingDraw, w.boolO)
      dOpt(name + isOfferingRematch, player(_).isOfferingRematch, w.boolO)
      dOpt(name + isProposingTakeback, player(_).isProposingTakeback, w.boolO)
      dOpt(name + blurs, player(_).blurs, w.intO)
    }

    (addUa(setBuilder.toList), unsetBuilder.toList)
  }

  private def addUa(sets: List[Set]): List[Set] = sets match {
    case Nil  => Nil
    case sets => (Game.BSONFields.updatedAt -> BSONJodaDateTimeHandler.write(DateTime.now)) :: sets
  }
}
