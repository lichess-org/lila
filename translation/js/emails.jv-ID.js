"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("Kanggo hubungi kita, mangga nggunakake %s.");i['common_note']=s("Iki minangka email layanan sing ana gandhengane karo panggunaan sampeyan %s.");i['common_orPaste']="(Klik ora bisa? Coba pasting menyang browser panjenengan!)";i['emailChange_click']="Kanggo konfirmasi sampeyan duwe akses menyang email iki, klik link ing ngisor iki:";i['emailChange_intro']="Sampeyan wis njaluk ngganti alamat email sampeyan.";i['emailChange_subject']=s("Konfirmasi alamat email anyar, %s");i['emailConfirm_click']="Klik link kanggo ngaktifake akun Lichess panjenengan:";i['emailConfirm_ignore']="Yen sampeyan durung ndhaftar Karo Liches sampeyan bisa aman ditolak lewat pesen iki.";i['emailConfirm_subject']=s("Konfirmasi panjenengan lichess.org akun, %s");i['logInToLichess']=s("Log ing kanggo lichess.org, %s");i['passwordReset_clickOrIgnore']="Yen sampeyan nggawe panjaluk iki, klik link ing ngisor iki. Yen ora, sampeyan bisa nglirwakake email iki.";i['passwordReset_intro']="Kita nampa panjaluk kanggo ngreset sandhi kanggo akun sampeyan.";i['passwordReset_subject']=s("Ngreset panjenengan lichess.org sandhi, %s");i['welcome_subject']=s("Sugeng rawuh lichess.org, %s");i['welcome_text']=s("Sampeyan wis kasil digawe akun ing https://lichess.org.\n\nPunika kaca profil: %1$s  sampeyan bisa nggawe pribadi ing %2$s.\n\nSeneng - seneng, lan muga-muga potongan-potongan sampeyan tansah nemokake dalan menyang raja mungsuh sampeyan!")})()