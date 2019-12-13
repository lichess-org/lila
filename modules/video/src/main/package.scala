package lila

package object video extends PackageObject {

  type Target = Int
  type Tag    = String
  type Lang   = String

  private[video] def logger = lila.log("video")
}
