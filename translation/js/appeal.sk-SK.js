"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.appeal)window.i18n.appeal={};let i=window.i18n.appeal;i['accountMuted']="Posielanie správ je u Vášho konta zakázané.";i['accountMutedInfo']=s("Prečítajte si naše %s. Nedodržanie komunikačných pokynov môže viesť k zákazu posielania správ.");i['arenaBanned']="Vášmu účtu je zakázané vstupovať do arén.";i['blogRules']="pravidlá blogovania";i['boosterMarked']="Váš účet je označený kvôli manipulácii s ratingom.";i['boosterMarkedInfo']="Definujeme to ako úmyselnú manipuláciu s ratingom prostredníctvom úmyselného prehrávania partií alebo hrania proti inému účtu, ktorý partie úmyselne prehráva.";i['cleanAllGood']="Váš účet nie je označený ani obmedzený. Všetko je v poriadku!";i['closedByModerators']="Váš účet bol zrušený moderátormi.";i['communicationGuidelines']="komunikačné smernice";i['engineMarked']="Váš účet je označený kvôli externej pomoci v partiách.";i['engineMarkedInfo']=s("Definujeme to ako využívanie akejkoľvek externej pomoci na posilnenie svojich vedomostí a/alebo schopností kalkulácie s cieľom získať neférovú výhodu nad súperom. Viac informácií nájdete na stránke %s.");i['excludedFromLeaderboards']="Vaše konto bolo vylúčené z rebríčkov.";i['excludedFromLeaderboardsInfo']="Definujeme to ako používanie akéhokoľvek neférového spôsobu, ako sa dostať do rebríčka.";i['fairPlay']="Fair Play";i['hiddenBlog']="Vaše blogy boli skryté moderátormi.";i['hiddenBlogInfo']=s("Nezabudnite si znovu prečítať naše %s.");i['playTimeout']="Máte dočasný zákaz hrania.";i['prizeBanned']="Váš účet má zakázané zúčastňovať sa turnajov so skutočnými cenami."})()