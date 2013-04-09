package lila.user

final class Setting(ctx: Context) {

  def theme = Theme(get("theme"))
  def theme(value: String) = set("theme", Theme(value).toString) 

  def sound = get("sound", "false").parseBoolean | false
  def sound(value: String) = set("sound", value) 

  def chat = get("chat", "true").parseBoolean | true
  def chat(value: String) = set("chat", value) 

  def bg = get("bg", "light")
  def bg(value: String) = set("bg", value) 

  private def get(name: String, default: String = ""): String =
    ctx.req.session get name orElse {
      ctx.me flatMap (_ setting name) map (_.toString)
    } getOrElse default

  // then, set LilaCookie.session(name, value.toString)(ctx.req)
  private def set(name: String, value: String): Funit =
    ctx.me zmap { m ⇒ UserRepo.saveSetting(m.id, name, value.toString) } 
}

object Setting {

  def apply(ctx: Context) = new Setting(ctx)
}
