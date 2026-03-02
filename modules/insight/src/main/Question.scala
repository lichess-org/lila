package lila.insight

case class Question[X](
    dimension: InsightDimension[X],
    metric: InsightMetric,
    filters: List[Filter[?]] = Nil
):
  def filter(f: Filter[?]) = copy(filters = f :: filters)

  def withMetric(m: InsightMetric) = copy(metric = m)

  def monKey = s"${dimension.key}_${metric.key}"

case class Filter[A](
    dimension: InsightDimension[A],
    selected: List[A]
):

  def isEmpty = selected.isEmpty || selected.sizeIs == InsightDimension.valuesOf(dimension).size

  import reactivemongo.api.bson.*

  def matcher: BSONDocument = InsightDimension.filtersOf(dimension, selected)

object Filter:

  import lila.insight.InsightDimension as D
  import lila.rating.PerfType

  def apply(color: Color): Filter[Color] = Filter(D.Color, List(color))
  def apply(perfType: PerfType): Filter[PerfType] = Filter(D.Perf, List(perfType))
  def apply(perfTypes: Iterable[PerfType]): Filter[PerfType] = Filter(D.Perf, perfTypes.toList)
