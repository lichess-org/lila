package chess

trait DecayingRecorder {
  def record(value: Float): DecayingStats
}

final case class DecayingStats(
    mean: Float,
    deviation: Float,
    decay: Float
) extends DecayingRecorder {
  def record(value: Float): DecayingStats = {
    val delta = mean - value

    copy(
      mean = value + decay * delta,
      deviation = decay * deviation + (1 - decay) * Math.abs(delta)
    )
  }

  def record[T](values: Iterable[T])(implicit n: Numeric[T]): DecayingStats =
    values.foldLeft(this) { (s, v) =>
      s record n.toFloat(v)
    }
}

final case class EmptyDecayingStats(
    deviation: Float,
    decay: Float
) extends DecayingRecorder {
  def record(value: Float) =
    DecayingStats(
      mean = value,
      deviation = deviation,
      decay = decay
    )
}

object DecayingStats {
  val empty = EmptyDecayingStats
}
