"""
数据客户端
- 实时行情/概况：腾讯单股 API（快速）
- K线历史：腾讯 ifzq API
- 批量/分析数据：akshare（后续 LLM 使用）
"""
import logging
from typing import Optional
import requests

logger = logging.getLogger(__name__)

HEADERS = {"User-Agent": "Mozilla/5.0"}


def normalize_symbol(symbol: str) -> str:
    """转为腾讯格式：sh600519 / sz000001"""
    s = symbol.strip().upper()
    s = s.removeprefix("SH").removeprefix("SZ").removeprefix("HK").removeprefix("US")
    if s.startswith("6"):
        return f"sh{s}"
    elif s.startswith("0") or s.startswith("3") or s.startswith("2"):
        return f"sz{s}"
    elif s.startswith("8") or s.startswith("4") or s.startswith("92"):
        return f"bj{s}"
    return s


def get_quote(symbol: str) -> Optional[dict]:
    """获取实时行情（腾讯单股 API，毫秒级）。"""
    try:
        code = normalize_symbol(symbol)
        r = requests.get(f"http://qt.gtimg.cn/q={code}", headers=HEADERS, timeout=10)
        r.encoding = "gbk"
        text = r.text.strip()
        if "=" not in text:
            logger.warning("腾讯行情返回空: %s", symbol)
            return None

        parts = text.split('"')[1].split("~")
        if len(parts) < 40:
            logger.warning("腾讯行情格式异常: %s", symbol)
            return None

        return {
            "代码": parts[2],
            "名称": parts[1],
            "最新价": parts[3],
            "昨收": parts[4],
            "今开": parts[5],
            "最高": parts[33],
            "最低": parts[34],
            "成交量": parts[6],
            "成交额": parts[37],
            "涨跌幅": parts[32],
            "涨跌额": parts[31],
            "总市值": parts[45] if len(parts) > 45 else "0",
            "流通市值": parts[44] if len(parts) > 44 else "0",
            "市盈率-动态": parts[39] if len(parts) > 39 else "0",
        }
    except Exception as e:
        logger.error("get_quote 异常: %s -> %s", symbol, e)
        return None


def get_history(symbol: str, start_date: str = "", end_date: str = "") -> Optional[list[dict]]:
    """获取日线 K 线历史（腾讯 ifzq API）。"""
    try:
        code = normalize_symbol(symbol)
        url = "http://web.ifzq.gtimg.cn/appstock/app/fqkline/get"
        params = {"param": f"{code},day,,,500,qfq"}
        r = requests.get(url, params=params, headers=HEADERS, timeout=15)
        data = r.json()

        code_key = code.lower()
        klines = None
        if code_key in data.get("data", {}):
            day_data = data["data"][code_key]
            klines = day_data.get("qfqday") or day_data.get("day")

        if not klines:
            logger.warning("历史数据为空: %s", symbol)
            return None

        records = []
        for k in klines:
            records.append({
                "date": str(k[0]), "open": str(k[1]), "close": str(k[2]),
                "high": str(k[3]), "low": str(k[4]), "volume": str(k[5]),
            })
        return records
    except Exception as e:
        logger.error("get_history 异常: %s -> %s", symbol, e)
        return None


def get_overview(symbol: str) -> Optional[dict]:
    """获取基本面概况（复用腾讯行情数据）。"""
    return get_quote(symbol)

