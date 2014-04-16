package lila.common

case class LightUser(id: String, name: String, title: Option[String]) {

  def titleName = s"${title getOrElse ""} $name"
  def titleNameHtml = s"${title getOrElse ""}&nbsp;$name"
}
