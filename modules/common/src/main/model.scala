package lila.common

case class ApiVersion(value: Int) extends AnyVal with IntValue {
  def v1 = value == 1
  def v2 = value == 2
}

case class AssetVersion(value: Int) extends AnyVal with IntValue

case class MaxPerPage(value: Int) extends AnyVal with IntValue
