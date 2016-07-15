package lila.common

case class ApiVersion(value: Int) extends AnyVal with IntValue {
  def v1 = value == 1
}
