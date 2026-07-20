package lila.shutup

/**   - words do not partial match. "anal" will NOT match "analysis".
  *   - en, es, de and fr words are automatically pluralized. "tit" will also match "tits", "cabron" will also
  *     match "cabrones" etc.
  */
private object Dictionary:

  def en = dict("""
(burn|die|rot) in hell
(f++|ph)(u++|e++|a++)c?k(er|r|u|k|t|ing?|ign|en|tard?|face|of+|e?d|)
go to hell
(kill|hang|ne[ck]+|unalive) my ?self
[ck]um(shot|)
[ck]unt(ing|)
abortion
adol(f|ph)
afraid
anal(plug|se[ckx]+|)
anus
ape
arse(hole|wipe|)
ass
as+(hole|fag)
autist(ic|)
au[cs]+hwitz
bastard?
be[ea]++t?ch
b(iy?t?|t)ch
blow(job|)
blumpkin
bol+o[ck]+
bomb (yo)?ur?(self)?
boner
boob
bozo
brain(dea?d|les+)
bugg+er
bu[ck]+ake
bull?shit
cancer
che[ae]+t(ing|e[dr]+|)
chess(|-|_)bot(.?com)?
chi[ck]+en
chink
chitter
clit(oris|)
clown
co[ck]+(su[ck]+(er|ing)|)
condom
coon
coward?
crip+le
cry(baby|ing|)
cu[ck]+(old|)
cun+ilingu
di[ck]+(head|face|su[ck]+er|)
dildo
dog+ystyle
dogshit
douche(bag|)
downsie?
dumb(as+|)
dyke
engine
foreskin
fu*+[ck]+(e+[dr]+|t|ing?|ign|tard?|face|of+|)
gangbang(e?d|)
gay
go (and )?bomb
gobshite?
gook
gypo
gypsy
handjob
hitler++
hom+o(se[ckx]+ual|)
honkey
hooker
horny
humping
[iİ]diot
incest
jerk
jiz+(um|)
kill (you|u)
labia
lamer?
lesbo
lo++ser++
mag+ot
masturbat(e?[dr]+|ion|ing|)
mf\b
milf
molest(e?[dr]+|ing|)
mong
monkey
mor+on+
mother(fu[ck]+(er|)|)
mthrf[ck]+r
murder (you|u)
na+zi+
[nv]i+g+[ae]+[hr]?
nonce
noo++b
nutsa[ck]+
pa?edo((f|ph)ile|)
pajeet
paki
pathetic
pa?ederast
pe*nis?
pig
pimp
piss
poo+f
poo+n
poo++p(face|)
po?rn(hub|)
pos\b
pri[ck]+
prostitute
punani
pus+(i|y|ie|)
que+r
rapist
rat\b
rect(al|um)
retard(ed|)
rimjob
run
sandbag+(e[dr]+|ing|)
scared?
schlong
screw(e?d|)
scrotum
scum(bag|)
semen
se([ck]+s|x)
shagg?(e?d|)
she?male
shi?t(z|e|y|ty|bag|)
sis+y
s[ck]ank
slag
slave
slut
spastic
spaz
sperm
spi[ck]+
spooge
spunk
smurf+(e?[dr]+|ing|)
stfu
stupid
subhuman
suicided?
su[ck]+er
su[ck]+(e?d|) m[ey]
sybau
ter+orist
tit+(ies|y|)(fu[ck]+(er|)|)
tos+er
tran+(y|ie)
trash
turd
twat
unalive (you|u)
vag
vagin(a|al|)
vibrator
vulva
w?hore
wan[ck]+(er|)
we+a+k
wetba[ck]+
wog
(you|u) su[ck]+(e?d|)
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
(|от|с|про|за|на)пизд(|а|ы|е|у|еть|ел|ела|ели|ить|ил|ила|или|ошить?|ошил|ошила|ошили|охать|охал|охала|охали|юк|юлить?|юлил|юлила|юлили|ярить?|ярил|ярила|ярили|яхать|яхал|яхала|яхали|ячить?|ячил|ячила|ячили|якать|якал|якала|якали|ец|ецу|еца|ецкий|абол|атый|атая|атое|ос|он)
(|отъ?|вы|до|за|у|про|на|съ?)(е|ё)ба(|л|ла|ли|ло|лся|льник|ть|на?|нн?о|нул|нула|нулся|нн?ый?|нько|н[её]тесь|н[её]шь?ся)
(|отъ?|вы|до|за|у|про)(е|ё)б(аш)?(у|и|ите|ете|ёте|ёшся|ешся|етесь|ётесь|ить|еть)
(вы|до|за|у|про)(е|ё)бывае?(ть?ся|тесь|те|л|ла)
анус(|ам?и?|у|ов|е|ы)
аутист(|ам?и?|у|ов|е|ы)
бля(|дь?|ди|де|динам?и?|дине?|дство|ть?)
вшив(ый|ая|ое|ые|ыми?)
с+(ы|цы|ци)кун
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
m[aá]tate
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
frocio
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
(f|ph)a++g+([oi]t|)
(gets?|from|of|by|on|wish|(yo)?u) cancer
(go(es|s)?|pl(ea)?[sz]e?) (a?nd)? ?(die|burn|suicide)
(ho?pe|wish)(es|s)? ((yo?)?[uy](r (famil[yi]|dad|m[ou]m|mother)s?)?( and )?)++ (die|burn)
(kill|hang|ne[ck]+|murder|unalive)s? ?(yo?)?[uyi]r? ?(self|famil[yi]|m[ou]m|(fa|mo)ther(fucker)?)
cancers? (for|to)
gas the
g?kys
gets? (bombed|shot)
k y s
(l|1|ı|\|)<ys
nig+er
rap(ed?|e?ing)
rope
subhuman
""")

  private def dict(words: String) = words.linesIterator.filter(_.nonEmpty)
