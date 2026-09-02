package lila.ui

enum MarkdownRealm(val maxImageCount: Int, val imageDesignWidth: Int, val toastUi: Boolean):
  case cms extends MarkdownRealm(100, 900, true)
  case blog extends MarkdownRealm(10, 800, true)
  case forum extends MarkdownRealm(5, 864, false)
  case broadcast extends MarkdownRealm(2, 800, false)
  case team extends MarkdownRealm(2, 768, false)
  def key = toString

object MarkdownRealm:
  val byKey = values.mapBy(_.key)
