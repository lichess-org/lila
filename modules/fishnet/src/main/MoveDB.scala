package lila.fishnet

private final class MoveDB {

  import Work.Move

  private val maxSize = 300

  private val coll = scala.collection.mutable.Map.empty[Work.Id, Move]

  def get = coll.get _

  def add(move: Move): Unit = if (coll.size < maxSize) coll += (move.id -> move)

  def update(move: Move): Unit = if (coll contains move.id) coll += (move.id -> move)

  def delete(move: Move): Unit = coll -= move.id

  def contains = coll.contains _

  def exists = coll.values.exists _

  def find = coll.values.filter _

  def count = coll.values.count _

  def size = coll.size

  def oldestNonAcquired = coll.foldLeft(none[Move]) {
    case (acc, (_, m)) if m.nonAcquired => Some {
      acc.fold(m) { a =>
        if (m.createdAt isBefore a.createdAt) m else a
      }
    }
  }

  def updateOrGiveUp(move: Move) =
    if (move.isOutOfTries) {
      logger.warn(s"Give up on move $move")
      delete(move)
    }
    else update(move)
}
