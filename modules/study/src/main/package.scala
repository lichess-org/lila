package lila

package object study extends PackageObject {

  private[study] val logger = lila.log("study")

  private[study] type ChapterMap = Map[lila.study.Chapter.Id, lila.study.Chapter]
}
