package lila.game

import org.joda.time.DateTime
import reactivemongo.bson._

private[game] object GameDiff {

  type Set = (String, BSONValue)
  type Unset = (String, BSONBoolean)

  def apply(a: Game, b: Game): (List[Set], List[Unset]) = {

    val setBuilder = scala.collection.mutable.ListBuffer[Set]()
    val unsetBuilder = scala.collection.mutable.ListBuffer[Unset]()

    def d[A, C <: BSONValue](name: String, getter: Game => A)(implicit writer: BSONWriter[A, C]) {
      dd(name, getter, (a: A) => a)(writer)
    }

    def dd[A, B, C <: BSONValue](name: String, getter: Game => A, toBson: A ⇒ B)(implicit writer: BSONWriter[B, C]) {
      val (va, vb) = (getter(a), getter(b))
      if (va != vb) {
        if (vb == None || vb == null || vb == "") unsetBuilder += (name -> BSONBoolean(true))
        else setBuilder += name -> writer.write(toBson(vb))
      }
    }

    import Game.BSONFields._
    val w = lila.db.BSON.writer

    d(binaryPieces, _.binaryPieces)
    d(status, _.status.id)
    d(turns, _.turns)
    dd(castleLastMoveTime, _.castleLastMoveTime, CastleLastMoveTime.castleLastMoveTimeBSONHandler.write) 
    dd(check, _.check, (x: Option[chess.Pos]) => x map (_.toString)) 
    dd(positionHashes, _.positionHashes, w.strO) 
    for (i ← 0 to 1) {
      import Player.BSONFields._
      val name = "p." + i + "."
      val player: Game => Player = if (i == 0) (_.whitePlayer) else (_.blackPlayer)
      d(name + isWinner, player(_).isWinner) 
      d(name + lastDrawOffer, player(_).lastDrawOffer)
      dd(name + isOfferingDraw, player(_).isOfferingDraw, w.boolO)
      dd(name + isOfferingRematch, player(_).isOfferingRematch, w.boolO)
      dd(name + isProposingTakeback, player(_).isProposingTakeback, w.boolO)
      d(name + blurs, player(_).blurs) 
      d(name + moveTimes, player(_).moveTimes) 
    }
    // a.c foreach { c ⇒
    //   d("c.c", _.c.get.c)
    //   d("c.w", _.c.get.w)
    //   d("c.b", _.c.get.b)
    //   d("c.t", _.c.get.t) // timer
    // }

    (setBuilder.toList, unsetBuilder.toList)
  }
}
