package lila.coach

case class Question(
  yAxis: Metric,
  xAxis: Dimension[_],
  filters: List[Filter[_]])

case class Filter[A](
  dimension: Dimension[A],
  selected: List[A])

case class Answer(
  question: Question,
  values: List[Value])

case class Value(
  name: String,
  value: Double)
