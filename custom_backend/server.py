import os, sqlite3, uuid, time
from pathlib import Path
from flask import Flask, request, jsonify, send_from_directory
import requests

APP = Flask(__name__)
DB = os.getenv("GIFTS_DB", "custom_gifts.db")
UPLOAD_DIR = Path(os.getenv("GIFTS_UPLOAD_DIR", "uploads"))
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

ADMIN_USER_ID = int(os.getenv("ADMIN_USER_ID", "0"))
GIFT_CHANNEL_URL = os.getenv("GIFT_CHANNEL_URL", "")
GIFT_CHANNEL_ID = os.getenv("GIFT_CHANNEL_ID", "")
TELEGRAM_BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")

def db():
    c = sqlite3.connect(DB)
    c.row_factory = sqlite3.Row
    return c

def init():
    c = db()
    c.executescript("""
    CREATE TABLE IF NOT EXISTS users(
      user_id INTEGER PRIMARY KEY,
      stars INTEGER NOT NULL DEFAULT 0,
      premium_until INTEGER NOT NULL DEFAULT 0,
      banned INTEGER NOT NULL DEFAULT 0,
      spam INTEGER NOT NULL DEFAULT 0,
      muted INTEGER NOT NULL DEFAULT 0
    );
    CREATE TABLE IF NOT EXISTS gifts(
      id TEXT PRIMARY KEY,
      creator_id INTEGER NOT NULL,
      title TEXT NOT NULL,
      stars INTEGER NOT NULL,
      video_url TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      enabled INTEGER NOT NULL DEFAULT 1
    );
    CREATE TABLE IF NOT EXISTS settings(
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS purchases(
      id TEXT PRIMARY KEY,
      gift_id TEXT NOT NULL,
      buyer_id INTEGER NOT NULL,
      creator_id INTEGER NOT NULL,
      stars INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    );
    """)
    c.commit(); c.close()

def caller_id():
    try:
        return int(request.headers.get("X-App-User-Id", "0"))
    except Exception:
        return 0

def ensure_user(uid):
    c=db()
    c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(uid,))
    c.commit(); c.close()

def is_admin(uid):
    return uid != 0 and ADMIN_USER_ID != 0 and uid == ADMIN_USER_ID

def telegram_member(uid):
    if not TELEGRAM_BOT_TOKEN or not GIFT_CHANNEL_ID:
        return False
    try:
        r=requests.get(
            f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/getChatMember",
            params={"chat_id": GIFT_CHANNEL_ID, "user_id": uid}, timeout=10)
        data=r.json()
        status=data.get("result",{}).get("status")
        return status in ("creator","administrator","member")
    except Exception:
        return False

@APP.get("/")
def index():
    return jsonify(status="ok", message="Messenger backend ishlayapti")

@APP.get("/api/gifts/channel")
def gift_channel():
    return jsonify(url=GIFT_CHANNEL_URL, configured=bool(GIFT_CHANNEL_URL), channel_id=GIFT_CHANNEL_ID)

@APP.get("/api/gifts")
def gifts():
    c=db()
    rows=c.execute("SELECT * FROM gifts WHERE enabled=1 ORDER BY created_at DESC").fetchall()
    c.close()
    return jsonify([dict(r) for r in rows])

@APP.get("/api/admin/gifts")
def admin_gifts():
    if not is_admin(caller_id()): return jsonify(error="forbidden"),403
    c=db(); rows=c.execute("SELECT id,title,creator_id,stars,video_url,created_at FROM gifts ORDER BY created_at DESC").fetchall(); c.close()
    return jsonify([dict(r) for r in rows])

@APP.post("/api/gifts")
def create_gift():
    uid=caller_id()
    if not uid or not telegram_member(uid):
        return jsonify(error="channel_required", channel=GIFT_CHANNEL_URL),403
    title=request.form.get("title","Custom Gift").strip()[:80] or "Custom Gift"
    try: stars=max(1,int(request.form.get("stars","1")))
    except Exception: return jsonify(error="invalid_stars"),400
    f=request.files.get("video")
    if not f: return jsonify(error="video_required"),400
    gift_id=uuid.uuid4().hex
    filename=gift_id+".mp4"
    f.save(UPLOAD_DIR/filename)
    url="/uploads/"+filename
    c=db()
    c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(uid,))
    c.execute("INSERT INTO gifts(id,creator_id,title,stars,video_url,created_at) VALUES(?,?,?,?,?,?)",
              (gift_id,uid,title,stars,url,int(time.time())))
    c.commit(); c.close()
    return jsonify(ok=True,id=gift_id,title=title,stars=stars,video_url=url,channel=GIFT_CHANNEL_URL)

@APP.post("/api/gifts/purchase")
def purchase():
    uid=caller_id()
    data=request.get_json(silent=True) or {}
    gid=str(data.get("gift_id",""))
    c=db()
    g=c.execute("SELECT * FROM gifts WHERE id=? AND enabled=1",(gid,)).fetchone()
    if not g: c.close(); return jsonify(error="gift_not_found"),404
    if uid==g["creator_id"]: c.close(); return jsonify(error="cannot_buy_own_gift"),400
    c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(uid,))
    buyer=c.execute("SELECT stars FROM users WHERE user_id=?",(uid,)).fetchone()
    if buyer["stars"] < g["stars"]:
        c.close(); return jsonify(error="not_enough_stars",required=g["stars"],balance=buyer["stars"]),400
    purchase_id=uuid.uuid4().hex
    c.execute("UPDATE users SET stars=stars-? WHERE user_id=?",(g["stars"],uid))
    c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(g["creator_id"],))
    c.execute("UPDATE users SET stars=stars+? WHERE user_id=?",(g["stars"],g["creator_id"]))
    c.execute("INSERT INTO purchases(id,gift_id,buyer_id,creator_id,stars,created_at) VALUES(?,?,?,?,?,?)",
              (purchase_id,gid,uid,g["creator_id"],g["stars"],int(time.time())))
    c.commit(); c.close()
    return jsonify(ok=True,purchase_id=purchase_id,creator_id=g["creator_id"],stars=g["stars"])

@APP.post("/api/admin/action")
def admin_action():
    admin=caller_id()
    if not is_admin(admin): return jsonify(error="forbidden"),403
    data=request.get_json(silent=True) or {}
    action=data.get("action","")
    c=db()
    try:
        if action in ("add_stars","remove_stars"):
            uid=int(data.get("user_id")); amount=max(0,int(data.get("amount")))
            c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(uid,))
            delta=amount if action=="add_stars" else -amount
            c.execute("UPDATE users SET stars=MAX(0,stars+?) WHERE user_id=?",(delta,uid))
        elif action=="give_premium":
            uid=int(data.get("user_id")); days=max(1,int(data.get("amount")))
            c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(uid,))
            now=int(time.time())
            cur=c.execute("SELECT premium_until FROM users WHERE user_id=?",(uid,)).fetchone()["premium_until"]
            c.execute("UPDATE users SET premium_until=? WHERE user_id=?",(max(cur,now)+days*86400,uid))
        elif action in ("ban","unban","spam","unspam","mute","unmute"):
            uid=int(data.get("user_id")); field={"ban":"banned","unban":"banned","spam":"spam","unspam":"spam","mute":"muted","unmute":"muted"}[action]
            val=0 if action.startswith("un") else 1
            c.execute("INSERT OR IGNORE INTO users(user_id) VALUES(?)",(uid,))
            c.execute(f"UPDATE users SET {field}=? WHERE user_id=?",(val,uid))
        elif action in ("premium_price","stars_price"):
            c.execute("INSERT INTO settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value",
                      (action,str(int(data.get("value")))))
        elif action=="create_gift_channel":
            pass
        else:
            return jsonify(error="unknown_action"),400
        c.commit()
    finally:
        c.close()
    return jsonify(ok=True)

@APP.get("/uploads/<path:name>")
def uploads(name):
    return send_from_directory(UPLOAD_DIR, name)

if __name__=="__main__":
    init()
    APP.run(host="0.0.0.0", port=int(os.getenv("PORT","10000")))
else:
    init()
