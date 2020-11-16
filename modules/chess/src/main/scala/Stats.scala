package chess

// Welford's numerically stable online variance.
//
sealed trait Stats {
  def samples: Int
  def mean: Float
  def variance: Option[Float]
  def record(value: Float): Stats
  def +(o: Stats): Stats

  def record[T](values: Iterable[T])(implicit n: Numeric[T]): Stats =
    values.foldLeft(this) { (s, v) =>
      s record n.toFloat(v)
    }

  def stdDev = variance.map { Math.sqrt(_).toFloat }

  def total = samples * mean
}

final protected case class StatHolder(
    samples: Int,
    mean: Float,
    sn: Float
) extends Stats {
  def variance = (samples > 1) option sn / (samples - 1)

  def record(value: Float) = {
    val newSamples = samples + 1
    val delta      = value - mean
    val newMean    = mean + delta / newSamples
    val newSN      = sn + delta * (value - newMean)

    StatHolder(
      samples = newSamples,
      mean = newMean,
      sn = newSN
    )
  }

  def +(o: Stats) =
    o match {
      case EmptyStats => this
      case StatHolder(oSamples, oMean, oSN) => {
        val invTotal = 1f / (samples + oSamples)
        val combMean = {
          if (samples == oSamples) (mean + oMean) * 0.5f
          else (mean * samples + oMean * oSamples) * invTotal
        }

        val meanDiff = mean - oMean

        StatHolder(
          samples = samples + oSamples,
          mean = combMean,
          sn = sn + oSN + meanDiff * meanDiff * samples * oSamples * invTotal
        )
      }
    }
}

protected object EmptyStats extends Stats {
  val samples  = 0
  val mean     = 0f
  val variance = None

  def record(value: Float) =
    StatHolder(
      samples = 1,
      mean = value,
      sn = 0f
    )

  def +(o: Stats) = o
}

object Stats {
  val empty = EmptyStats

  def apply(value: Float)                    = empty.record(value)
  def apply[T: Numeric](values: Iterable[T]) = empty.record(values)
}
