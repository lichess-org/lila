package lila.ui
package practice

import lila.core.id.StudyId
import lila.core.i18n.I18nKey

/* modules/activity needs to manipulate practice study objects,
 * but I don't want it to depend on modules/practice.
 * This stuff would usually belong in modules/core, but it also needs I18nKey,
 * which is from modules/coreI18n.
 */
trait Study:
  val id: StudyId
  val name: I18nKey
  def slug: String

type Studies = StudyId => Option[Study]
type GetStudies = () => Fu[Studies]
