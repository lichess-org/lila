package lila

package object blog extends PackageObject with WithPlay {

  private[blog] def logger = lila.log("blog")
}
