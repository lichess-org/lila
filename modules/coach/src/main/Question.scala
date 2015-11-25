package lila.coach

case class Question(
  yAxis: Metric,
  xAxis: Dimension,
  filters: List[Dimension])

case class Answer(
  question: Question,
  values: List[Value])

case class Value(
  name: String,
  value: Double)
