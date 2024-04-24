package lila.ui

final class KitchenSink(
    val formHelper: FormHelper,
    val dateHelper: DateHelper,
    val numberHelper: NumberHelper,
    val i18nHelper: I18nHelper,
    val htmlHelper: HtmlHelper,
    val userHelper: UserHelper
):
  export formHelper.*
  export userHelper.{ *, given }
  export i18nHelper.{ *, given }
  export dateHelper.*
  export numberHelper.*
  export htmlHelper.*
