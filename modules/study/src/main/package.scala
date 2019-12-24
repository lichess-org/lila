package lila

import com.github.blemale.scaffeine.AsyncLoadingCache

package object study extends PackageObject {

  private[study] val logger = lila.log("study")

  private[study] type ChapterMap = Map[lila.study.Chapter.Id, lila.study.Chapter]

  private[study] type LightStudyCache =
    AsyncLoadingCache[lila.study.Study.Id, Option[lila.study.Study.LightStudy]]
}
