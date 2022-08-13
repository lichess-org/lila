package views.html
package analyse

import controllers.routes
import lila.api.{ Context, ExternalEngine }
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object external {
  def apply(prompt: ExternalEngine.Prompt)(implicit ctx: Context) = views.html.base.layout(
    title = "External engine",
    moreCss = cssTag("oauth"),
    moreJs = embedJsUnsafe(
      """setTimeout(() => lichess.load.then(() => {
  const btn = document.getElementById('engine-authorize');
  btn.removeAttribute('disabled');
  btn.classList.remove('disabled');
  btn.addEventListener('click', () => {
    lichess.storage.set('ceval.external', btn.getAttribute('data-ceval-external'));
    location.href = '/analysis';
  });
}), 2000);"""
    )
  ) {
    main(cls := "oauth box box-pad")(
      div(cls := "oauth__top")(
        oAuth.authorize.ringsImage,
        h1("External engine (alpha)"),
        strong(code(prompt.url.origin))
      ),
      p("Do you want to use this external engine on your device?"),
      form3.actions(
        a(href := routes.UserAnalysis.index)("Cancel"),
        button(
          cls                         := "button disabled",
          disabled                    := true,
          id                          := "engine-authorize",
          attr("data-ceval-external") := prompt.toJson.toString
        )("Authorize")
      ),
      oAuth.authorize.footer(prompt.url.origin, isDanger = false)
    )
  }
}
