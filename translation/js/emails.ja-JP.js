"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.emails)window.i18n.emails={};let i=window.i18n.emails;i['common_contact']=s("当方へのご連絡には %s をお使いください。");i['common_note']=s("これは %s のご利用に関するメールです。");i['common_orPaste']="（クリックでうまくいかない場合、URLをブラウザにペーストしてください）";i['emailChange_click']="このメールアドレスが利用できると確認するため、下のリンクをクリックしてください。";i['emailChange_intro']="あなたはメールアドレスの変更を申請しました。";i['emailChange_subject']=s("%s さん、新しいメールアドレスの確認を");i['emailConfirm_click']="リンクをクリックして Lichess のアカウントを有効化してください。";i['emailConfirm_ignore']="もし Lichess に登録した覚えがなければ、無視して大丈夫です。";i['emailConfirm_subject']=s("%s さん lichess.org のアカウントご確認を");i['logInToLichess']=s("%s さん lichess.org にログインをどうぞ");i['passwordReset_clickOrIgnore']="申請した覚えがあれば下のリンクをクリックしてください。そうでないなら無視して大丈夫です。";i['passwordReset_intro']="あなたの Lichess アカウントのパスワード変更申請を受信しました。";i['passwordReset_subject']=s("%s さん、lichess.org のパスワードをリセットして");i['welcome_subject']=s("%s さん Lichess.org へようこそ");i['welcome_text']=s("https://lichess.org にアカウントを作成しました。\nプロフィールページは %1$s です。%2$s でさまざまな設定ができます。\nお楽しみください、そしてあなたの駒が相手のキングに迫れますように！")})()