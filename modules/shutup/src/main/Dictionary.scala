package lila.shutup

/**   - words do not partial match. "anal" will NOT match "analysis".
  *   - en, es, de and fr words are automatically pluralized. "tit" will also match "tits", "cabron" will also
  *     match "cabrones" etc.
  */
private object Dictionary:

  def en = dict("""
(f++|ph)(u++|e++|a++)c?k(er|r|u|k|t|ing?|ign|en|tard?|face|off?|e?d|)
(f|ph)agg?([oi]t|)
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
boner
boob
braindea?d
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
douche(bag|)
downsie?
dumb(ass?|)
dyke
engine
fck(er|r|u|k|t|ing?|ign|tard?|face|off?|e?d)
foreskin
gangbang(e?d|)
gay
gobshite?
gook
gypo
handjob
hitler++
homm?o(sexual|)
honkey
hooker
horny
humping
idiot
incest
jerk
jizz?(um|)
kill (you|u)
labia
lamer?
lesbo
lo++ser
masturbat(ed?|ion|ing)
milf
molest(er|ed|)
mong
monkey
moron
mother(fuc?k(er|)|)
mthrfckr
nazi
nigg?
nigg?ah?
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
(|на|по)ху(й|ю|я|ям|йня|йло|йла|йлу)
(|от)муд(охать|охал|охала|охали|аки?|акам|азвону?)
(|от|по)cос(и|ать|ала?|)
(|от|с)пизд(а|ы|е|у|ить|ил|ила|или|ошить|ошил|ошила|ошили|охать|охал|охала|охали|юлить|юлил|юлила|юлили|ярить|ярил|ярила|ярили|яхать|яхал|яхала|яхали|ячить|ячил|ячила|ячили|якать|якал|якала|якали|ец|ецкий|абол|атый)
(|отъ?|вы|до|за|у|про)(е|ё)ба(л|ла|ли|ло|лся|льник|ть|на|нул|нула|нулся|нн?ый)
(ё|е)бл(а|о|у|ану?)
(|за|отъ?|у)ебись
(|на|вы)ебнуть?ся
аутист
blyat
свинья
Убейся
p[ie]d[aoe]?r
uebok
анус
бля(|дь|ди|де|динам?|дине|дство|ть)
вы[её]бывае?(ть?ся|тесь)
г[ао]ндон(|у|ам?|ы|ов)
гнид(|ам?|е|у|ы)
д[ао]лбо[её]б(у|ам?|ы|ов)
даун(|у|ам?|ы|ов)
д[еи]бил(|ам?|ы|у|ов)
дерьм(а|о|вый|вая|вое)
к[ао]з(|е|ё)л(ам?|у|ы)
лопух
лох(|у|и|ам?)
лошар(|ам?|е|у|ы)
лузер(|ам?|у|ов|ы)
идиот(|ам?|ы|у|ов)
[оа]хуе(|л|ла|ли|ть|нн?о)
педерасты?
пид(о|а|)р(а|ы|у|ам|асы?|асам?|ов|)
поебень
придур(ок|кам?|ков|ки)
[сc][уy][кk](а|a|и|е|у|ам)
твар(ь|и|е|ина|ине|ину|ины)
тупиц(|ам?|ы|е)
ублюд(ок|кам?|ков|ку)
у(ё|е)бищ(е|а|ам|у)
ху(ё|е)(во|сос)
ху[еи]т(а|е|ы)
читак(и|ам?|у|ов)
читер(|ила?|ить?|ишь?|ша|ы|ам?|у|ов)
чмо(|шник)
шмар(ам?|е|ы)
шлюх(|ам?|е|и)
г[оа]вн[оа]ед(|ам?|е|у|ом|ов|ами|ах|ы)
г[оа]внюк(|ам?|е|у|ом|ов|ами|ах|и)
г[оа]вн(а|е|у|ом?)
сперм(а|у|ой|е)
(|отъ?|вы|до|за|у|про)(е|ё)б(аш)?(у|и|ите)
член[оа]сос(|а|у|ом|е|ы|ов|ами?|ах|ка|ке|ки|ку|кой)
""")

  def es = dict("""
cabr[oó]na?
cag[oó]n
ching(ue|a)
chupame
cobarde
est[úu]pid[ao]
idiota
imbecil
madre
maric[oó]na?
maric[ao]
mierda
moduler[ao]
payas[ao]
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
coglione
cretino
di merda
figa
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
encul[eé]
fdp
pd
pute
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
ezik
gerizekal[iı]
ibne
ka[sş]ar
oç
orospu( ([çc]o[çc]u[ğg]?u|evlad[ıi]))?
piç(lik)?
pu[sş]t
salak
s[ii̇]kecem
sikiyonuz
s[ii̇]kt[ii̇]r
yarra[gğ][iı] yediniz
""")

  def critical = dict("""
cancer
(go|pl(ea)?[sz]e?) (a?nd)? ?(die|burn|suicide)
(ho?pe|wish) ((yo?)?[uy](r (famil[yi]|m[ou]m|mother))?( and )?)++ (die|burn)s?
(die|burn)s? irl
(kill|hang|neck) ?(yo?)?[uyi]r? ?(self|famil[yi]|m[ou]m|mother)
kys
nigg?[ae]r
rap(ed?|e?ing)
""")

  private def dict(words: String) = words.linesIterator.filter(_.nonEmpty)
