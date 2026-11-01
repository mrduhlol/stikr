import createJellyfish, { meta } from './assets/ascii/pieces/jellyfish.js';
import { KEYS } from './assets/ascii/lib/kit.js';

const canvas = document.getElementById('jellyfish-background');
const motion = matchMedia('(prefers-reduced-motion: reduce)');

function startArtwork() {
  if (!canvas) return () => {};
  const context = canvas.getContext('2d', { alpha: false });
  if (!context) return () => {};
  const frameAt = createJellyfish();
  let request = 0, width = 0, height = 0, lastFrame = -Infinity, disposed = false;
  let fps = innerWidth < 650 ? 12 : 18;
  const began = performance.now();
  const ramp = ' .:-=+*#%@';
  const gray = Array.from({length:10}, (_,i) => { const v=90+i*15;return `rgb(${v},${v},${v})`; });

  function draw(seconds) {
    const frame = frameAt(seconds);
    const text = frame.text.split('\n');
    const colors = frame.color.split('\n');
    // Fit the character plane without stretching or cropping the silhouettes.
    const scale = Math.min(width / (meta.cols*8), height / (meta.rows*14));
    const cellWidth=8*scale, cellHeight=14*scale;
    const left=(width-meta.cols*cellWidth)/2, top=(height-meta.rows*cellHeight)/2;
    context.fillStyle='#000000'; context.fillRect(0,0,width,height);
    context.font=`${14*scale}px monospace`; context.textBaseline='top';
    for(let y=0;y<meta.rows;y++) for(let x=0;x<meta.cols;x++) {
      const key=KEYS.indexOf(colors[y][x]);
      // The library uses keys 0–11 for the blue water and 42–43 for particles.
      // Draw only jellyfish geometry, with new monochrome characters and ink.
      if(key<12 || key>=42 || text[y][x]===' ') continue;
      const shade=(key-12)%10;
      const character=ramp[Math.max(1,Math.round((shade+2)/11*9))];
      context.fillStyle=gray[shade];
      context.fillText(character,left+x*cellWidth,top+y*cellHeight);
    }
  }
  function resize() {
    width=innerWidth; height=innerHeight;
    const ratio=Math.min(devicePixelRatio || 1,1.5);
    canvas.width=Math.round(width*ratio); canvas.height=Math.round(height*ratio);
    context.setTransform(ratio,0,0,ratio,0,0);
    lastFrame=-Infinity; draw(16);
  }
  function tick(now) {
    if(disposed || document.hidden || motion.matches) { request=0;return; }
    if(now-lastFrame>=1000/fps) {
      const start=performance.now();draw(16+(now-began)/1000*.55);lastFrame=now;
      if(performance.now()-start>24) fps=8;
    }
    request=requestAnimationFrame(tick);
  }
  function update() {
    cancelAnimationFrame(request);request=0;
    if(motion.matches) draw(16);
    else if(!document.hidden && !disposed) request=requestAnimationFrame(tick);
  }
  resize();update();
  addEventListener('resize',resize,{passive:true});
  document.addEventListener('visibilitychange',update);
  motion.addEventListener('change',update);
  return () => {
    disposed=true;cancelAnimationFrame(request);
    removeEventListener('resize',resize);document.removeEventListener('visibilitychange',update);motion.removeEventListener('change',update);
  };
}
let stopArtwork=startArtwork();
addEventListener('pagehide',()=>stopArtwork());
addEventListener('pageshow',event=>{if(event.persisted)stopArtwork=startArtwork();});

const demo=document.getElementById('app-demo');
let requestedTab=null;
function openDemoTab(tab) {
  requestedTab=tab;
  demo?.contentWindow?.postMessage({type:'stikr-demo-tab',tab},location.origin);
}
document.querySelectorAll('[data-demo-tab]').forEach(button=>button.addEventListener('click',()=>openDemoTab(button.dataset.demoTab)));
demo?.addEventListener('load',()=>{if(requestedTab)openDemoTab(requestedTab);});
const actions=document.getElementById('hero-actions');
if(actions && 'IntersectionObserver' in window) {
  const observer=new IntersectionObserver(entries=>{
    document.body.classList.toggle('has-sticky',!entries[0].isIntersecting && entries[0].boundingClientRect.top<0);
  });
  observer.observe(actions);
  addEventListener('pagehide',()=>observer.disconnect(),{once:true});
}
