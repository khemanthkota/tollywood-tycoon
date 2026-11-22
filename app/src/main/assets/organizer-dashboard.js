(()=>{'use strict';
const room=document.getElementById('room');
if(!room||room.dataset.dashboardObserver)return;
const auction=document.getElementById('hostControls');
if(!auction)return;
room.dataset.dashboardObserver='1';
const initialize=()=>{
if(auction.classList.contains('hide')||room.dataset.dashboardReady)return;
room.dataset.dashboardReady='1';
const children=[...room.children];
const summary=children.find(el=>el.classList.contains('card'));
const studio=document.getElementById('playerControls');
const players=children.filter(el=>el.classList.contains('card')&&el!==summary).find(el=>el.querySelector('#players'));
const results=children.filter(el=>el.classList.contains('card')&&el!==summary&&el!==players).find(el=>el.querySelector('#history'));
if(!summary||!auction||!studio||!players||!results)return;
const style=document.createElement('style');
style.textContent=`
#room.organizer-dashboard{max-width:1180px;margin:22px auto;padding:0}
#room .dashboard-heading{display:flex;align-items:center;justify-content:space-between;gap:14px;margin:0 0 16px;padding:18px 22px;border:1px solid #4b566b;border-radius:18px;background:linear-gradient(120deg,#262d3e,#1b2231)}
#room .dashboard-brand{font-size:19px;font-weight:850;letter-spacing:.04em;color:#ffd080}
#room .dashboard-subtitle{font-size:12px;color:#aebbd0;margin-top:4px}
#room .dashboard-stage{font-size:12px;color:#d9e2f1;padding:8px 12px;border:1px solid #59657a;border-radius:999px;background:#121a29}
#room .dashboard-layout{display:block}
#room .dashboard-content{min-width:0}
#room .dashboard-home{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}
#room .dashboard-home[hidden],#room .dashboard-panel[hidden]{display:none!important}
#room .dashboard-home-card{display:flex;align-items:center;gap:14px;min-height:100px;padding:18px;text-align:left;border:1px solid #46536a;border-radius:16px;background:linear-gradient(120deg,#252d3e,#1b2231);color:#f8f1e8}
#room .dashboard-home-card strong{display:block;color:#ffda9b;font-size:17px}
#room .dashboard-home-card small{display:block;margin-top:5px;color:#b9c4d5;font-weight:400}
#room .dashboard-home-icon{font-size:28px}
#room .dashboard-page-head{display:flex;align-items:center;gap:14px;margin:0 0 14px;padding:12px;border:1px solid #46536a;border-radius:14px;background:#171e2b}
#room .dashboard-back{flex:0 0 auto;margin:0;padding:10px 14px;background:#30394c;color:#ffda9b;border:1px solid #59657a}
#room .dashboard-page-title{font-size:18px;font-weight:800;color:#ffda9b}
#room .dashboard-panel>.card,#room .dashboard-panel>section.card{margin:0 0 14px;padding:20px;border-radius:16px;background:#222a39;border:1px solid #4c586e}
#room .dashboard-panel h3{margin-top:0;color:#ffda9b;font-size:18px}
#room #hostControls label{margin:12px 0}
#room #hostControls .row{margin-top:12px}
#room #startGame{width:100%;min-height:52px;font-size:17px}
#room #allowTv{width:auto}
#room #players p,#room #history p{padding:10px 12px;margin:8px 0;border-radius:9px;background:#171f2e;border:1px solid #354158}
#room #playerControls #budget{font-size:20px;font-weight:800;color:#ffda9b}
#room .dashboard-action-card{padding:16px;margin:0 0 14px;border-radius:14px;background:#171f2e;border:1px solid #46536a}
#room .dashboard-action-card button{margin:0}
#room .dashboard-empty-note{color:#9eacc2;font-size:13px}
#room .dashboard-invite{margin-top:12px;padding:12px;border:1px solid #46536a;border-radius:12px;background:#171f2e}
#room .dashboard-invite>summary{cursor:pointer;color:#ffda9b;font-weight:700}
#room .dashboard-invite #qrSection{padding-top:12px}
#room .dashboard-invite #joinQR{display:block;width:150px;height:150px;object-fit:contain}
#room .dashboard-start{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:0 0 14px;padding:15px;border:1px solid #a8754b;border-radius:14px;background:linear-gradient(110deg,#38283a,#252b3a)}
#room .dashboard-start strong{display:block;color:#ffda9b}
#room .dashboard-start small{display:block;margin-top:4px;color:#b9c4d5}
#room .dashboard-start button{margin:0;min-width:130px;min-height:46px}
@media(max-width:720px){
 #room.organizer-dashboard{margin:12px 0}
 #room .dashboard-heading{padding:12px;display:block}
 #room .dashboard-brand{font-size:15px;line-height:1.25;max-width:100%;overflow-wrap:anywhere}
 #room .dashboard-subtitle{display:none}
 #room .dashboard-stage{display:inline-block;margin-top:8px;font-size:10px;white-space:normal}
 #room .dashboard-home{grid-template-columns:1fr}
 #room .dashboard-home-card{min-height:76px;padding:14px}
 #room .dashboard-page-head{gap:10px}
 #room .dashboard-page-title{font-size:16px}
 #room .dashboard-panel>.card,#room .dashboard-panel>section.card{padding:13px}
 #room .dashboard-start{align-items:stretch;flex-direction:column}
 #room .dashboard-start button{width:100%}
 #room .dashboard-invite #qrSection{min-width:0}
 #room .dashboard-invite #joinLink{font-size:12px}

}`;
document.head.appendChild(style);
room.classList.add('organizer-dashboard');
const heading=document.createElement('header');heading.className='dashboard-heading';
heading.innerHTML='<div><div class="dashboard-brand">🎬 CINEMA TYCOON · ORGANIZER STUDIO</div><div class="dashboard-subtitle">Manage the room, auction, movie production and results</div></div><div class="dashboard-stage" id="dashboardStage">ROOM CONTROL</div>';
const stage=heading.querySelector('#dashboardStage');
const layout=document.createElement('div');layout.className='dashboard-layout';
const content=document.createElement('div');content.className='dashboard-content';
const specs=[['lobby','▦','Lobby','Room, invitations and players'],['auction','🔨','Auction','Celebrity selection and live bidding'],['studio','🎬','Movie studio','Production and submissions'],['results','🏆','Results','Final standings and ROI']];
const panels={};
const home=document.createElement('section');home.className='dashboard-home';home.setAttribute('aria-label','Organizer pages');
specs.forEach(([key,icon,label,description])=>{
 const button=document.createElement('button');button.type='button';button.className='dashboard-home-card';
 button.innerHTML='<span class="dashboard-home-icon">'+icon+'</span><span><strong>'+label+'</strong><small>'+description+'</small></span>';
 home.append(button);button.addEventListener('click',()=>activate(key));
 const panel=document.createElement('section');panel.className='dashboard-panel';panel.dataset.panel=key;panel.hidden=true;
 const pageHead=document.createElement('header');pageHead.className='dashboard-page-head';
 const back=document.createElement('button');back.type='button';back.className='dashboard-back';back.textContent='← Back';
 back.addEventListener('click',showHome);
 const pageTitle=document.createElement('div');pageTitle.className='dashboard-page-title';pageTitle.textContent=label;
 pageHead.append(back,pageTitle);panel.append(pageHead);content.appendChild(panel);panels[key]=panel;
});
content.append(home);
function showHome(){Object.values(panels).forEach(p=>p.hidden=true);home.hidden=false;stage.textContent='ORGANIZER HOME';}
function activate(key){home.hidden=true;Object.entries(panels).forEach(([name,p])=>p.hidden=name!==key);stage.textContent=(specs.find(x=>x[0]===key)?.[2]||'ORGANIZER').toUpperCase();try{history.pushState({organizerPage:key},'','#organizer-'+key)}catch{}}
window.addEventListener('popstate',()=>showHome());
const invite=document.createElement('details');invite.className='dashboard-invite';
invite.innerHTML='<summary>📲 Invite players · QR code and join link</summary>';
const qr=document.getElementById('qrSection');if(qr)invite.append(qr);
summary.append(invite);
panels.lobby.append(summary);
const startCard=document.createElement('section');startCard.className='dashboard-start';
startCard.innerHTML='<div><strong>Ready to begin?</strong><small>Start the auction when all players have joined.</small></div>';
const startButton=document.getElementById('startGame');if(startButton)startCard.append(startButton);
panels.lobby.append(startCard,players);
panels.auction.append(auction);
const studioActions=document.createElement('section');studioActions.className='dashboard-action-card';studioActions.innerHTML='<h3>Production phase</h3><p class="dashboard-empty-note">Move the room into movie-making when the auction is complete.</p>';
const movies=document.getElementById('movies');if(movies)studioActions.append(movies);
panels.studio.append(studioActions,studio);
const resultActions=document.createElement('section');resultActions.className='dashboard-action-card';resultActions.innerHTML='<h3>Finalize the game</h3><p class="dashboard-empty-note">Publish the final studio results when submissions are ready.</p>';
const showResults=document.getElementById('results');if(showResults)resultActions.append(showResults);
panels.results.append(resultActions,results);
room.replaceChildren(heading,layout);layout.append(content);showHome();
const phase=document.getElementById('phase');
if(phase&&stage){const observer=new MutationObserver(()=>{stage.textContent=phase.textContent.replace(/^Stage:\s*/,'').toUpperCase()||'ROOM CONTROL'});observer.observe(phase,{childList:true,subtree:true,characterData:true});stage.textContent=phase.textContent.replace(/^Stage:\s*/,'').toUpperCase()||'ROOM CONTROL'}
};
if(!auction.classList.contains('hide'))initialize();
else{const observer=new MutationObserver(()=>{if(!auction.classList.contains('hide')){observer.disconnect();initialize()}});observer.observe(auction,{attributes:true,attributeFilter:['class']})}
})();