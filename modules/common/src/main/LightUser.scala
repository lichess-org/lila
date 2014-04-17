package lila.common

case class LightUser(id: String, name: String, title: Option[String]) {

  def titleName = title.fold(name)(_ + " " + name)
  def titleNameHtml = title.fold(name)(_ + "&nbsp;" + name)
}
