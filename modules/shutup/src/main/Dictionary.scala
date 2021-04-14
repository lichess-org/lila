package lila.shutup

/** - words are automatically pluralized. "tit" will also match "tits"
  * - words are automatically leetified. "tit" will also match "t1t", "t-i-t", and more.
  * - words do not partial match. "anal" will NOT match "analysis".
  */
private object Dictionary {

  def en = dict("""
(f+|ph)(u{1,}|a{1,}|e{1,})c?k(er|r|u|k|ed|d|t|ing?|ign|en|tard?|face|off?|)
(f|ph)agg?(ot|)
cock(suc?ker|)
[ck]um(shot|)
[ck]unt(ing|)
abortion
adol(f|ph)
afraid
anal(plug|sex|)
anus
arse(hole|wipe|)
ass
ass?(hole|fag)
aus?c?hwitz
bastard?
be[ea]+ch
bit?ch
blow(job|)
blumpkin
bollock
boner
boob
bugger
buk?kake
bull?shit
cancer
cheat(ing|ed|er|s|)
chess(|-|_)bot(.?com)?
chicken
chink
clit(oris|)
clown
cock
condom
coon
coward?
cunn?ilingu
dic?k(head|face|suc?ker|)
dildo
dogg?ystyle
douche(bag|)
dyke
engine
fck(er|r|u|k|ed|d|t|ing?|ign|tard?|face|off?|)
foreskin
gangbang
gay
gobshite?
gook
gypo
handjob
hitler+
homm?o(sexual|)
honkey
hooker
(ho?pe (yo)?u(r family)? die|dies? irl)
horny
humping
idiot
incest
jerk
jizz?(um|)
(kill|hang) (yo)?ur(self| family)
kys
labia
lamer?
lesbo
loo?ser
masturbat(e|ion|ing)
milf
molest
moron
mother(fuc?k(er|)|)
mthrfckr
nazi
nigg?(er|a|ah)
nonce
noo+b
nutsac?k
pa?edo((f|ph)ile|)
paki
pathetic
pa?ederast
penis
pig
pimp
piss
poof
poon
poo+p(face|)
porn
pric?k
prostitute
punani
puss(i|y|ie|)
queer
rape
rapist
rect(al|um)
retard
rimjob
run
sandbagg?(er|ing|)
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
slag
slave
slut
spastic
spaz
sperm
spick
spooge
spunk
smurff?(er|ing|)
stfu
stupid
suicide
suck m[ey]
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
w?hore?
wanc?k(er|)
weak
wetback
wog
(you|u) suck
""")

  def ru = dict("""
(|на|по)хуй(|ня)
(|от)муд(охать|охал|охала|охали|аки?|акам|азвону?)
(|от)хер(|ачить|ово|ня)
(|от|по)cос(и|ать|ала?|)
(|от|с)пизд(а|ы|е|у|ить|ил|ила|или|ошить|ошил|ошила|ошили|охать|охал|охала|охали|юлить|юлил|юлила|юлили|ярить|ярил|ярила|ярили|яхать|яхал|яхала|яхали|ячить|ячил|ячила|ячили|якать|якал|якала|якали|ец|ецкий|абол|атый)
(|отъ|вы|до|за|у)(е|ё)ба(л|ла|ли|лся|льник|ть|на|нул|нула|нулся|нн?ый)
(ё|е)бл(а|о|у|ану?)
p[ie]d[aoe]?r
анус
бля(|дь|ди|де|динам?|дине|дство)
г(а|о)ндон(|у|ам?|ы)
дерьм(а|о|вый|вая|вое)
Лопух
Лох(|и|ам)
охуе(л|ла|ли|ть|нн?о)
педерасты?
пид(о|а)р(а|ы|у|ам|асы?|асам?)
пидр
поебень
[сc][уy][кk](а|a|и|е|у|ам)
у(ё|е)бищ(е|а|ам)
ху(ё|е)(во|сос)
хуит(а|е|ы)
читак(и|ам|у)
читер(|ила?|ить?|ишь?|ша|ы|ам?|у)
""")

  def es = dict("""
cabr[oó]na?
chupame
est[úu]pid[ao]
imbecil
maric[oó]n
mierda
pendejo
put[ao]
trampa
trampos[ao]
verga
""")

  def it = dict("""
baldracca
bastardo
cazzo
cretino
di merda
figa
putt?ana
stronzo
troia
vaffanculo
""")

  def hi = dict("""
(madar|be?hen|beti)chod
chut(iya|)
gaa?nd
""")

  def fr = dict("""
fdp
pd
triche
tricheur
""")

  def de = dict("""
angsthase
arschloch
bl(ö|oe|o)dmann?
drecksa(u|ck)
ficker
fotze
hurensohn
mistkerl
neger
pisser
schlampe
schwanzlutscher
schwuchtel
trottel
wichser
""")

  def tr = dict("""
am[iı]na (koyay[iı]m|koy?dum)
amc[iı]k
anan[iı]n am[iı]
ann?an[iı](zi)? s[ii̇]k[eii̇]y[ii̇]m
aptal
beyinsiz
bok yedin
gerizekal[iı]
ibne
ka[sş]ar
orospu( ([çc]o[çc]u[ğg]?u|evlad[ıi]))?
piç(lik)?
pu[sş]t
salak
s[ii̇]kecem
sikiyonuz
s[ii̇]kt[ii̇]r
yarra[gğ][iı] yediniz
""")

  private def dict(words: String) = words.linesIterator.filter(_.nonEmpty)
}
