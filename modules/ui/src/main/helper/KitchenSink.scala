package lila.ui

final class KitchenSink(
    val formHelper: FormHelper,
    val dateHelper: DateHelper,
    val numberHelper: NumberHelper,
    val i18nHelper: I18nHelper,
    val stringHelper: StringHelper,
    val htmlHelper: HtmlHelper,
    val userHelper: UserHelper,
    val gameHelper: GameHelper
):
  export formHelper.*
  export userHelper.{ *, given }
  export i18nHelper.{ *, given }
  export dateHelper.*
  export numberHelper.*
  export htmlHelper.*
  export stringHelper.*
