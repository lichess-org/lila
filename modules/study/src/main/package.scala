package lila

import lila.hub.TrouperMap

package object study extends PackageObject {

  private[study] val logger = lila.log("study")

  private[study] type ChapterMap = Map[lila.study.Chapter.Id, lila.study.Chapter]

  private[study] type LightStudyCache = lila.memo.AsyncCache[lila.study.Study.Id, Option[lila.study.Study.LightStudy]]
}
