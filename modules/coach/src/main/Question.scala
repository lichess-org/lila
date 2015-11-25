package lila.coach

case class Question[X, Dim <: Dimension[X]](
  yAxis: Metric,
  xAxis: Dim,
  filters: List[Filter[_]])

case class Filter[A](
  dimension: Dimension[A],
  selected: List[A])

case class Answer[X, Dim <: Dimension[X]](
  question: Question[X, Dim],
  values: List[Value[X]])

case class Value[X](
  x: X,
  value: Double)
