(function(){'use strict';const q=id=>document.getElementById(id);function click(id){const x=q(id);if(x)x.click();}
q('launchCore').addEventListener('click',()=>click('coreBtn'));
q('launchConstructed').addEventListener('click',()=>click('constructedBtn'));
q('launchChoices').addEventListener('click',()=>click('choicesBtn'));
q('launchFates').addEventListener('click',()=>click('fatesBtn'));
q('launchInuyasha')?.addEventListener('click',()=>window.InuYashaTabletop?.open());
q('launchLimited').addEventListener('click',()=>window.FocusedMagicUI?.openLimitedMode());
})();
