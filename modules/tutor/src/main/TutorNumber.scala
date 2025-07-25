package lila.tutor

import alleycats.Zero
import scalalib.Iso
import chess.IntRating

import lila.analyse.AccuracyPercent
import lila.insight.ClockPercent

trait TutorNumber[V]:
  val iso: Iso.DoubleIso[V]
  def double(v: V) = iso to v
  given Zero[ValueCount[V]] = Zero(ValueCount[V](iso.from(0), 0))
  def grade(a: V, b: V): Grade
  def mean(vs: Iterable[ValueCount[V]]): ValueCount[V] =
    vs.foldLeft((0d, 0)) { case ((sum, total), ValueCount(value, count)) =>
      (sum + iso.to(value) * count, total + count)
    } match
      case (sum, total) => ValueCount(iso.from(if total > 0 then sum / total else 0), total)
  def mean(a: ValueCount[V], b: ValueCount[V]): ValueCount[V] =
    if a.count < 1 && b.count < 1 then ValueCount(iso.from(0), 0)
    else
      ValueCount(
        iso.from((double(a.value) * a.count + double(b.value) * b.count) / (a.count + b.count)),
        a.count + b.count
      )
  def mean(a: Option[ValueCount[V]], b: Option[ValueCount[V]]): ValueCount[V] =
    mean(~a, ~b)

  def reverseCompare = new TutorNumber[V]:
    val iso = TutorNumber.this.iso
    override def grade(a: V, b: V) = TutorNumber.this.grade(b, a)
    override def mean(vs: Iterable[ValueCount[V]]): ValueCount[V] = TutorNumber.this.mean(vs)

object TutorNumber:

  given TutorNumber[GoodPercent] with
    val iso = summon[Iso[Double, GoodPercent]]
    def grade(a: GoodPercent, b: GoodPercent) = Grade.percent(a, b)
  given TutorNumber[AccuracyPercent] with
    val iso = summon[Iso[Double, AccuracyPercent]]
    def grade(a: AccuracyPercent, b: AccuracyPercent) = Grade.percent(a, b)
  given TutorNumber[IntRating] with
    val iso = Iso.double[IntRating](d => IntRating(roundToInt(d)), _.value.toDouble)
    def grade(a: IntRating, b: IntRating) = Grade((a.value - b.value) / 150d)
  given TutorNumber[ClockPercent] with
    val iso = Iso.double[ClockPercent](ClockPercent.fromPercent(_), _.value)
    def grade(a: ClockPercent, b: ClockPercent) = Grade.percent(a, b)
