"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Per cuntattà ci, aduprate %s.");i['common_note']=s("Hè un currieru elettronicu di u serviziu liatu à a vostra utilizazioni di %s.");i['common_orPaste']="(Ùn funziona micca u cliccu? Appicicate a leia in u vostru navigatore!)";i['emailChange_click']="Per cunfirmà ch\\' è vo avete un accessu à issu mail, clicate nantu à a leia inghjò:";i['emailChange_intro']="Avete dumandatu di cambià u vostru indirizzu elettronicu.";i['emailChange_subject']=s("Cunfirmate u vostru novu indiruzzu elettronicu, %s");i['emailConfirm_click']="Clicate nantu à a leia per validà u vostru contu Lichess:";i['emailConfirm_ignore']="S\\'è ùn site micca arregistratu nantu à Lichess, pudete ignurà u missaghju.";i['emailConfirm_subject']=s("Cunfirmate u vostru contu lichess.org, %s");i['logInToLichess']=s("Identificate vi nantu à lichess.org, %s");i['passwordReset_clickOrIgnore']="S\\'è vo avete fattu sta dumanda, clicate puru nantu à a leia inghjò. S\\' è ùn l\\' avete micca fattu, pudete ignurà stu currieru elettronicu.";i['passwordReset_intro']="Avemu ricivutu una dumanda par creà da torna a parolla d\\' intesa di u vostru contu.";i['passwordReset_subject']=s("Create da torna a vostra parolla d\\'intesa di lichess.org, %s");i['welcome_subject']=s("Benvenutu nantu à lichess.org, %s");i['welcome_text']=s("Avete creatu u vostru contu nantu à https://lichess.org. Eccu a vostra pagina di prufile: %1$s. Pudete persunalizà lu clichendu quì %2$s. Campate vi! Ch\\' elle trovinu sempre u so caminu versu u rè aversu e vostre pezze!")})()