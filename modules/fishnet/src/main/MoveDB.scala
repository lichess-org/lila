package lila.fishnet

import scala.concurrent.stm._

private final class MoveDB {

  import Work.Move

  private val maxSize = 1000

  type CollType = TMap[Work.Id, Move]

  private val coll: CollType = TMap.empty

  def get = coll.single.get _

  def add(move: Move)(implicit txn: InTxn): Unit =
    if (coll.size < maxSize) coll.put(move.id, move)

  def update(move: Move)(implicit tnx: InTxn): Unit =
    if (coll.contains(move.id)) coll.put(move.id, move)

  def delete(move: Move)(implicit tnx: InTxn): Unit =
    coll.remove(move.id)

  def transaction[A](f: InTxn => A): A = atomic { txn =>
    f(txn)
  }

  def contains = coll.single.contains _

  def exists = coll.single.values.exists _

  def find = coll.single.values.filter _

  def count = coll.single.values.count _

  def size = coll.single.size

  def updateOrGiveUp(move: Move) = transaction { implicit txn =>
    if (move.isOutOfTries) {
      logger.warn(s"Give up on move $move")
      delete(move)
    }
    else update(move)
  }
}
