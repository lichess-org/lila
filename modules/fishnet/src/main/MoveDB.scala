package lila.fishnet

private final class MoveDB {

  import Work.Move

  private val maxSize = 1000

  private val coll = scala.collection.mutable.Map[Work.Id, Move]()

  def add(move: Move) = if (coll.size < maxSize) coll += (move.id -> move)

  def get = coll.get _

  def contains = coll.contains _

  def exists = coll.values.exists _

  def find = coll.values.filter _

  def update(move: Move) = if (contains(move.id)) coll += (move.id -> move)

  def delete(move: Move) = coll -= move.id

  def count = coll.values.count _

  def giveUp(move: Move) = {
    log.warn(s"Give up on move $move")
    delete(move)
  }

  def updateOrGiveUp(move: Move) =
    if (move.isOutOfTries) giveUp(move) else update(move)
}
