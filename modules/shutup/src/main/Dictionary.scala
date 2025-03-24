package lila.shutup

/**   - words do not partial match. "anal" will NOT match "analysis".
  *   - en, es, de and fr words are automatically pluralized. "tit" will also match "tits", "cabron" will also
  *     match "cabrones" etc.
  */
private object Dictionary:

  def en = dict("""
(burn|die|rot) in hell
(f++|ph)(u++|e++|a++)c?k(er|r|u|k|t|ing?|ign|en|tard?|face|off?|e?d|)
go to hell
(kill|hang|neck) my ?self
[ck]um(shot|)
[ck]unt(ing|)
abortion
adol(f|ph)
afraid
anal(plug|sex|)
anus
ape
arse(hole|wipe|)
ass
ass?(hole|fag)
autist(ic|)
aus?c?hwitz
bastard?
be[ea]++t?ch
b(iy?t?|t)ch
blow(job|)
blumpkin
bollock
bomb (yo)?ur?(self)?
boner
boob
bozo
brain(dea?d|less?)
bugger
buk?kake
bull?shit
che[ae]t(ing|er|ed|)
chess(|-|_)bot(.?com)?
chicken
chink
chitter
clit(oris|)
clown
cock(suc?k(er|ing)|)
condom
coon
coward?
cripp?led?
cry(baby|ing|)
cunn?ilingu
dic?k(head|face|suc?ker|)
dildo
dogg?ystyle
dogshit
douche(bag|)
downsie?
dumb(ass?|)
dyke
engine
fck(er|r|u|k|t|ing?|ign|tard?|face|off?|e?d)
foreskin
gangbang(e?d|)
gay
go (and )?bomb
gobshite?
gook
gypo
gypsy
handjob
hitler++
homm?o(sexual|)
honkey
hooker
horny
humping
[iİ]diot
incest
jerk
jizz?(um|)
kill (you|u)
labia
lamer?
lesbo
lo++ser++
maggot
masturbat(ed?|ion|ing)
mf\b
milf
molest(er|ed|)
mong
monkey
morr?on
mother(fuc?k(er|)|)
mthrfckr
nazi
nigg?
nigg?a[hr]?
nonce
noo++b
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
poo++p(face|)
porn(hub|)
pric?k
prostitute
punani
puss(i|y|ie|)
queer
rapist
rat\b
rect(al|um)
retard(ed|)
rimjob
run
sandbagg?(er|ing|ed|)
scared?
schlong
screw(e?d|)
scrotum
scum(bag|)
semen
sex
shagg?(e?d|)
shemale
shit(z|e|y|ty|bag|)
sissy
skank
slag
slave
slut
spastic
spaz
sperm
spick
spooge
spunk
smurff?(er|ing|e?d|)
stfu
stupid
subhuman
suicided?
suc?ker
suck(e?d|) m[ey]
terrorist
tit(t?ies|ty|)(fuc?k|)
tosser
trann(y|ie)
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
(you|u) suck(e?d|)
""") ++ critical

  def ru = dict("""
suka
blyat
gandon
p[ie]d[aoe]?r
uebok
(|на|по)ху(й|и|ю|е|ё|ям?и?|йням?и?|йнёй|йней|йло|йла|йлу|йцам?и?|йцу|йцо|иням?и?|инёй|иней|ило|ила|илу)
(|от)муд[оа](хать|хал|хала|хали|ки?|кам?и?|ков|звону?)
(|от|по)cос(и|ать|ала?|ни|нуть?|нешь?|нёшь?)
(|от|с|про|за|на)пизд(|а|ы|е|у|еть|ел|ела|ели|ить|ил|ила|или|ошить?|ошил|ошила|ошили|охать|охал|охала|охали|юлить?|юлил|юлила|юлили|ярить?|ярил|ярила|ярили|яхать|яхал|яхала|яхали|ячить?|ячил|ячила|ячили|якать|якал|якала|якали|ец|ецу|еца|ецкий|абол|атый|атая|атое|ос|он)
(|отъ?|вы|до|за|у|про|на|съ?)(е|ё)ба(|л|ла|ли|ло|лся|льник|ть|на?|нн?о|нул|нула|нулся|нн?ый?|нько|н[её]тесь|н[её]шь?ся)
(|отъ?|вы|до|за|у|про)(е|ё)б(аш)?(у|и|ите|ете|ёте|ёшся|ешся|етесь|ётесь|ить|еть)
(вы|до|за|у|про)(е|ё)бывае?(ть?ся|тесь|те|л|ла)
анус(|ам?и?|у|ов|е|ы)
аутист(|ам?и?|у|ов|е|ы)
бля(|дь?|ди|де|динам?и?|дине?|дство|ть?)
вшив(ый|ая|ое|ые|ыми?)
г[ао]ндон(|ам?и?|у|ов|е|ы)
г[оа]вн(а|е|у|ом?)
г[оа]вн[оа]ед(|ам?и?|е|у|ом|ов|ами|ах|ы)
г[оа]внюк(|ам?и?|е|у|ом|ов|ами|ах|и)
гнид(|ам?и?|у|ов|е|ы)
д[ао]лб[ао][её]б(|ам?и?|у|ов|ы)
д[ао]лб[ао]нут(|ый|ые|ая|ой|ое|ым?и?)
даун(|ам?и?|у|ов|е|ы)
д[еи]бил(|ам?и?|у|ов|е|ы)
д[еи]рьм(а|о|у|овый|овая|овое|овые)
(ё|е)бл(ам?и?|о|у|ы|ану?|аны|анам?и?|ище|ищам?и?|я|ей)
жоп(ам?и?|у|ы|е)
заткни(сь|ся|тесь)
ид[ие]от(|ам?и?|у|ов|е|ы)
к[ао]з(|е|ё)л(|ам?и?|у|ов|ы)
лопух(|ам?и?|у|ов|е|и)
лох(|ам?и?|у|ов|е|и)
л[оа]шар(|ам?и?|у|ов|е|ы)
лузер(|ам?и?|у|ов|е|ы)
муд(ень|[ао]звон)(|ам?и?|у|ов|е|ы)
[оа]хуе(|л|ла|ли|ло|ть|ет|ешь?|нн?о|нн?а|нен|вать|вш[иы]й|вшая|вшое)
п[ие]д(о|а|е|)р(ас(тр?|)|)(а|у|ами?|оми?|е|ы|ов|ах|)
пидрил(|ам?и?|е|у|ы)
поебен[ьие]
(при|полу)дур(ок|кам?и?|ков|ки)
прогг?(ам?и?|и|ой|ою)
прогг?ер(|ам?и?|у|ов|ы)
проститутк(|ам?и?|у|е|и)
свинь(ям?и?|е|и)
сдохни
сперм(а|у|ой|е)
[сc][уy][кk](а|a|и|е|у|ами?)
сучк(ам?и?|е|и)
твар(ь|и|е|ина?|ине|ину|ины)
туп(|ой|ая|ое|ые|ым?и?)
тупиц(|ам?и?|у|е|ы)
убей(ся| себя)
ублюд(ок|кам?и?|ков|ку|ке)
у[еи]бан(|ам?и?|у|ов|е|и)
у(ё|е)бищ(е|ам?и?|у)
умри
урод(|ам?и?|у|ов|е|ы|ина?|ине|ину|ины)
ху(ё|е)(во|сос)(|ам?и?|у|ов|ы)
ху[еи]т(а|е|ы|у)
чит(|ам?и?|ов|ы)
читак(|ам?и?|у|ов|е|и)
читер(|ила?|ить?|ишь?|ша|ы|ам?и?|у|ов|е)
член(|ам?и?|у|ов|е)
член[оа]сос(|а|у|ом|е|ы|ов|ами?|ах|ка|ке|ки|ку|кой)
чмо(|шник)(|ам?и?|у|ов|е|и)
шалав(|ам?и?|у|е|ы)
шмар(|ам?и?|у|е|ы)
шлюх(|ам?и?|у|е|и)
яйцам?и?
""")

  def es = dict("""
bolud[oa]
cabr[oó]na?
cag[oó]n
ching(ue|a)
chupa ?pija
chupame
cobarde
est[úu]pid[ao]
gilipollas
hdp
hijo de (put\w*|per+a)
hijueputa
idiota
imbecil
madre
malparid[ao]
maric[oó]na?
maric[ao]
mierda
moduler[ao]
payas[ao]
pendejo
po(ll|y)a
put[ao]
putica
trampa
trampos[ao]
tu eres put\w*
verga
""")

  def it = dict("""
baldracca
bastardo
cazzo
coglione
cornutt?o
cretino
di merda
figa
li mortacci tua
putt?ana
stronzo
troia
vaffanculo
sparati
""")

  def hi = dict("""
(mada?r|mother|be?hen|beti)chod
bh?o?sdi?ke?
chut(iy[ae]|)
gaa?ndu?
""")

  def fr = dict("""
batard
connard
cr[eé]tin
encul[eé]r?
fdp
pd
pute
p[eé]d[eé]raste
salope
triche(ur|)
conn?ard?
""")

  def de = dict("""
angsthase
arschloch
bl(ö|oe|o)dmann?
drecksa(u|ck)
fick(|er)
fotze
hurensohn
mistkerl
neger
pisser
schlampe
schwanzlutscher
schwuchtel
trottel
untermensch
wi(chs|x++)er
""")

  def tr = dict("""
am[iı]na ((koy?dum)|(koya(y[iı]m|m))|(soka(y[iı]m|m))|([cç]aka(y[iı]m|m)))
amc[iı]k
anan[iı]n am[iı]
((ann?an[iı](z[iı])?)|(kar[iı]n[iı](z[iı])?)|(avrad[iı]n[iı](z[iı])?)|(bac[ıi]n[iı](z[iı])?)) (s[ii̇]k[ei](yim|cem|rim|m))
((ann?an(a|[iı]za))|(kar[iı]n(a|[iı]za))|(avrad[iı]n(a|[iı]za))|(bac[ıi]n(a|[iı]za))) ((koya(y[iı]m|m))|(soka(y[iı]m|m))|([cç]aka(y[iı]m|m)))
aptal
beyinsiz
bok yedin
ezik
gerizekal[iı]
göt
ibne
ka[sş]ar
oç+
[ou]r[ou]s[pb]u( ([çc]o[çc]u[ğg]?u|evlad[ıi]))?
piç(lik)?
pu[sş]t
salak
s[ii̇]k[ei](yim|cem|rim|m|k)
s[ii̇]kt[ii̇]r
yar+a[gğ][iı] yediniz
yar+ak kafa(l[iı]|s[iı])
""")

  def critical = dict("""
(die|burn)s? irl
(f|ph)a++gg?([oi]t|)
(go|pl(ea)?[sz]e?) (a?nd)? ?(die|burn|suicide)
(ho?pe|wish) ((yo?)?[uy](r (famil[yi]|m[ou]m|mother))?( and )?)++ (die|burn)s?
(kill|hang|neck) ?(yo?)?[uyi]r? ?(self|famil[yi]|m[ou]m|mother)
cancer
gas the
g?kys
get bombed
k y s
ky5
(l|1|ı|\|)<ys
n[1i]gg?er
rap(ed?|e?ing)
subhuman
""")

  private def dict(words: String) = words.linesIterator.filter(_.nonEmpty)
