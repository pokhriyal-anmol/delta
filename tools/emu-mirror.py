#!/usr/bin/env python3
"""Drive the Android emulator from a browser tab (including the Claude desktop app's browser pane).

    python3 tools/emu-mirror.py            # then open http://127.0.0.1:4391

Polls `adb screencap` for frames (about 3 per second) and forwards clicks, drags, scrolling and
buttons back through `adb input`. Binds to 127.0.0.1 only and runs a fixed set of adb commands
with validated arguments, so it is not a general remote shell.
"""
import json, os, re, subprocess, sys, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

SERIAL = os.environ.get("EMU_SERIAL", "emulator-5554")
PORT = int(os.environ.get("EMU_PORT", "4391"))
APP = "io.github.pokhriyal_anmol.f1"
APK = os.path.join(os.path.dirname(__file__), "..", "app/build/outputs/apk/debug/app-debug.apk")
KEYS = {"BACK": "KEYCODE_BACK", "HOME": "KEYCODE_HOME", "RECENTS": "KEYCODE_APP_SWITCH", "ENTER": "KEYCODE_ENTER", "DEL": "KEYCODE_DEL"}


def adb(*args, out=False, timeout=30):
    cmd = ["adb", "-s", SERIAL, *args]
    r = subprocess.run(cmd, capture_output=True, timeout=timeout)
    return r.stdout if out else (r.returncode, (r.stdout + r.stderr).decode(errors="replace").strip())


def num(q, k, lo=0, hi=5000):
    v = int(q[k][0])
    if not lo <= v <= hi:
        raise ValueError(k)
    return str(v)


PAGE = """<!doctype html><meta charset=utf-8><title>Delta emulator</title>
<style>
body{margin:0;background:#111;color:#ddd;font:13px system-ui;display:flex;gap:18px;justify-content:center;align-items:flex-start;padding:12px;height:100vh;box-sizing:border-box}
#wrap{height:calc(100vh - 24px);aspect-ratio:1080/2400;border-radius:22px;overflow:hidden;background:#000;position:relative;box-shadow:0 0 0 1px #333}
canvas{width:100%;height:100%;display:block;touch-action:none;cursor:pointer}
aside{display:flex;flex-direction:column;gap:8px;min-width:150px}
button{font:500 13px system-ui;background:#222;color:#eee;border:1px solid #333;border-radius:10px;padding:9px 12px;cursor:pointer;text-align:left}
button:hover{background:#2c2c2c}button:active{transform:scale(.97)}
#st{color:#888;font-size:12px;line-height:1.5}input{background:#181818;border:1px solid #333;color:#eee;border-radius:8px;padding:8px;width:100%;box-sizing:border-box}
</style>
<div id=wrap><canvas id=c width=1080 height=2400></canvas></div>
<aside>
<button data-a="key?k=BACK">Back</button><button data-a="key?k=HOME">Home</button><button data-a="key?k=RECENTS">Recents</button>
<button data-a="theme?m=yes">Dark theme</button><button data-a="theme?m=no">Light theme</button>
<button data-a="relaunch">Restart app</button><button data-a="install">Install latest build</button>
<input id=t placeholder="type, then Enter"><div id=st>connecting...</div>
</aside>
<script>
const c=document.getElementById('c'),x=c.getContext('2d'),st=document.getElementById('st');let n=0,t0=Date.now();
async function loop(){for(;;){try{const r=await fetch('/frame?'+Date.now());const b=await createImageBitmap(await r.blob());x.drawImage(b,0,0,1080,2400);n++;
 st.textContent=((n/((Date.now()-t0)/1000))).toFixed(1)+' fps'}catch(e){st.textContent='no frame: '+e;await new Promise(r=>setTimeout(r,1000))}}}
loop();
const pt=e=>{const r=c.getBoundingClientRect();return[Math.round((e.clientX-r.left)/r.width*1080),Math.round((e.clientY-r.top)/r.height*2400)]};
let d=null;c.addEventListener('pointerdown',e=>{d={p:pt(e),t:Date.now()};c.setPointerCapture(e.pointerId)});
c.addEventListener('pointerup',e=>{if(!d)return;const [a,b]=d.p,[u,v]=pt(e),ms=Math.max(80,Math.min(900,Date.now()-d.t));d=null;
 if(Math.hypot(u-a,v-b)<24)fetch(`/tap?x=${a}&y=${b}`);else fetch(`/swipe?x1=${a}&y1=${b}&x2=${u}&y2=${v}&ms=${ms}`)});
c.addEventListener('wheel',e=>{e.preventDefault();const [a,b]=pt(e),dy=Math.max(-900,Math.min(900,Math.round(e.deltaY*2)));
 fetch(`/swipe?x1=${a}&y1=${b}&x2=${a}&y2=${Math.max(5,Math.min(2395,b-dy))}&ms=180`)},{passive:false});
document.querySelectorAll('button').forEach(b=>b.onclick=()=>fetch('/'+b.dataset.a).then(r=>r.text()).then(t=>{if(t&&t!=='ok')st.textContent=t}));
document.getElementById('t').addEventListener('keydown',e=>{if(e.key==='Enter'){fetch('/text?s='+encodeURIComponent(e.target.value));e.target.value=''}});
</script>"""


class H(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def send(self, body, ctype="text/plain", code=200):
        b = body if isinstance(body, bytes) else body.encode()
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(b)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(b)

    def do_GET(self):
        u = urlparse(self.path)
        q = parse_qs(u.query)
        try:
            if u.path == "/":
                return self.send(PAGE, "text/html; charset=utf-8")
            if u.path == "/frame":
                return self.send(adb("exec-out", "screencap", "-p", out=True), "image/png")
            if u.path == "/tap":
                adb("shell", "input", "tap", num(q, "x", 0, 1080), num(q, "y", 0, 2400))
            elif u.path == "/swipe":
                adb("shell", "input", "swipe", num(q, "x1", 0, 1080), num(q, "y1", 0, 2400),
                    num(q, "x2", 0, 1080), num(q, "y2", 0, 2400), num(q, "ms", 50, 2000))
            elif u.path == "/key":
                adb("shell", "input", "keyevent", KEYS[q["k"][0]])
            elif u.path == "/text":
                s = q["s"][0]
                if not re.fullmatch(r"[A-Za-z0-9 .,@_-]{1,80}", s):
                    return self.send("only letters, digits and . , @ _ - are sent", code=400)
                adb("shell", "input", "text", s.replace(" ", "%s"))
            elif u.path == "/theme":
                m = q["m"][0]
                if m not in ("yes", "no"):
                    return self.send("bad", code=400)
                adb("shell", "cmd", "uimode", "night", m)
            elif u.path == "/relaunch":
                adb("shell", "am", "force-stop", APP)
                adb("shell", "am", "start", "-n", f"{APP}/.MainActivity")
            elif u.path == "/install":
                rc, msg = adb("install", "-r", os.path.abspath(APK), timeout=120)
                adb("shell", "am", "start", "-n", f"{APP}/.MainActivity")
                return self.send(msg.splitlines()[-1] if msg else "done")
            else:
                return self.send("not found", code=404)
            self.send("ok")
        except (KeyError, ValueError, IndexError):
            self.send("bad request", code=400)
        except subprocess.TimeoutExpired:
            self.send("adb timed out", code=504)
        except BrokenPipeError:
            pass


if __name__ == "__main__":
    print(f"emulator mirror for {SERIAL}: http://127.0.0.1:{PORT}", flush=True)
    ThreadingHTTPServer(("127.0.0.1", PORT), H).serve_forever()
