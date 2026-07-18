import asyncio, json, logging, threading, requests
from aiocqhttp import CQHttp
from http.server import HTTPServer, BaseHTTPRequestHandler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

YOUR_WEBHOOK = "http://localhost:8000/qq_msg"
bot = CQHttp()
send_queue = []

class SendHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        data = json.loads(self.rfile.read(int(self.headers.get('Content-Length', 0))))
        send_queue.append(data)
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'ok')
    log_message = lambda s,*a: None

@bot.on_startup
async def init():
    t = threading.Thread(target=lambda: HTTPServer(("0.0.0.0", 8081), SendHandler).serve_forever(), daemon=True)
    t.start()
    logger.info("发送接口 http://0.0.0.0:8081")
    logger.info("等待 NapCat 连接...")

@bot.on_message()
async def handle_msg(event):
    user_id = event.user_id
    raw = event.message
    text = " ".join(s["data"]["text"] for s in raw if s.get("type") == "text") if isinstance(raw, list) else str(raw).strip()
    logger.info(f"收到 [{user_id}]: {text}")
    try:
        requests.post(YOUR_WEBHOOK, json={"user_id": user_id, "message": text}, timeout=3)
    except Exception as e:
        logger.error(f"转发失败: {e}")
    # 处理发送队列
    while send_queue:
        item = send_queue.pop(0)
        try:
            await bot.send_private_msg(user_id=item['user_id'], message=item['message'])
            logger.info(f"已发送")
        except Exception as e:
            logger.error(f"发送失败: {e}")

bot.run(host="0.0.0.0", port=3003)