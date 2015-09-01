package lila.search

trait Query {

  def toJson: play.api.libs.json.JsObject
}
