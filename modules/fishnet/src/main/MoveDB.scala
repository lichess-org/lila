package lila.fishnet

import scala.concurrent.stm._

private final class MoveDB {

  import Work.Move

  private val maxSize = 1000

  private val coll = TMap.empty[Work.Id, Move]

  def get = coll.single.get _

  def add(move: Move): Unit = atomic { implicit txn =>
    if (coll.size < maxSize) coll.put(move.id, move)
  }

  def update(move: Move): Unit = atomic { implicit txn =>
    if (coll.contains(move.id)) coll.put(move.id, move)
  }

  def delete(move: Move): Unit = atomic { implicit txn =>
    coll.remove(move.id)
  }

  def contains = coll.single.contains _

  def exists = coll.single.values.exists _

  def find = coll.single.values.filter _

  def count = coll.single.values.count _

  def size = coll.single.size

  def giveUp(move: Move) = {
    log.warn(s"Give up on move $move")
    delete(move)
  }

  def updateOrGiveUp(move: Move) =
    if (move.isOutOfTries) giveUp(move) else update(move)
}
