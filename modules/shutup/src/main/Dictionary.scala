package lila.shutup

/** - words are automatically pluralized. "tit" will also match "tits"
  * - words are automatically leetified. "tit" will also match "t1t", "t-i-t", and more.
  * - words do not partial match. "anal" will NOT match "analysis".
  */
private object Dictionary {

  def en = dict("""
[ck]oc?k(y|suc?ker|)
[ck]um(shot|)
[ck]unt(ing|)
(f+|ph)(u{1,}|a{1,}|e{1,})c?k(er|r|u|k|ed|d|t|ing?|ign|en|tard?|face|off?|)
fck(er|r|u|k|ed|d|t|ing?|ign|tard?|face|off?|)
abortion
adol(f|ph)
afraid
anal(plug|sex|)
anus
arse(hole|wipe|)
ass
ass?(hole|fag)
aus?c?hwitz
ball
bastard?
be[ea]+ch
bewb
bimbo
bit?ch
blow
blowjob
blumpkin
bollock
boner
boob
bugger
buk?kake
bull?shit
cancer
cawk
cheat(ed|er|s|)
chess(|-|_)bot(.?com)?
chicken
chink
choad
clit
clitoris
clown
condom
coon
cock
cooter
cornhole
coward?
crap
cunn?ilingu
dic?k(head|face|suc?ker|)
dildo
dogg?ystyle
dong
douche(bag|)
dyke
engine
(f|ph)ag
(f|ph)agg?ot
fanny
(f|ph)art
foreskin
gangbang
gay
genital
genitalia
gobshite?
gook
gypo
handjob
hell
hitler+
homm?o(sexual|)
honkey
hooker
horny
humping
idiot
incest
jerk
jizz?(um|)
kaffir
kb
keyboard
kike
kys
labia
lamer?
lesbo
masturbat(e|ion|ing)
milf
molest
moron
mothers?
motherfuc?k(er|)
mthrfckr
muff
nazi
nigg?(er|a|ah)
nonce
nutsac?k
pa?edo
pa?edo(f|ph)ile
paki
pathetic
pecker
pederast
pen(1|i)s
pig
pimps?
piss
poof
poon
poop(face|)
porn
pric?k
pron
prostitute
punani
puss(i|y|ie|)
queef
queer
quim
rape
rapist
rect(al|um)
retard
rimjob
run
scare
schlong
screw
scrotum
scum(bag|)
semen
sex
shag
shemale
shit(z|e|y|ty|bag|)
sissy
sister
slag
slave
slut
spastic
spaz
sperm
spick
spoo
spooge
spunk
stfu
stripper
stupid
suicide
taint
tart
terrorist
tit(|t?ies|ty)(fuc?k)
tosser
trash
turd
twat
vag
vagin(a|al|)
vibrator
vulva
wanc?k(er|)
weak
wetback
w?hore?
wog
""")

  def ru =
    dict(
      """
сука
пизда
пидор(|ас)
педераст
pid(a|o|)r
Лох
Сосать
Лопух
Соси
анус
бля(|дь|ди|дина|дство)
дерьмо
(|отъ|вы|до|за|у)еба(л|ла|ли|лся|льник|ть|н|нул|нула|нулся)
у(ё|е)бище
(|от)муд(охать|охал|охала|охали|ак)
(|от|с)пизд(ить|ил|ила|или|ошить|ошил|ошила|ошили|охать|охал|охала|охали|юлить|юлил|юлила|юлили|ярить|ярил|ярила|ярили|яхать|яхал|яхала|яхали|ячить|ячил|ячила|ячили|якать|якал|якала|якали|ец|ецкий|абол|атый)
(|от)хер(|ачить|ово|ня)
охуе(л|ла|ли|ть)
поебень
ху(ё|е)(во|сос)
хуй(|ня)
читак
читер(|ила?|ить?|ишь|ша)
чмо
"""
    )

  private def dict(words: String) = words.linesIterator.filter(_.nonEmpty)
}
