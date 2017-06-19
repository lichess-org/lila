package lila.study

private case class MoveOpts(
  write: Boolean,
  sticky: Boolean,
  promoteToMainline: Boolean
)

private object MoveOpts {

  import play.api.libs.json._
  import lila.common.PimpedJson._

  def apply(o: JsObject): MoveOpts = {
    val d = (o obj "d") | Json.obj()
    MoveOpts(
      write = d.get[Boolean]("write") | true,
      sticky = d.get[Boolean]("sticky") | true,
      promoteToMainline = d.get[Boolean]("promote") | false
    )
  }
}
