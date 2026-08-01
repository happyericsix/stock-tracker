"""
quant_model.py —— 量化分析模块
基于技术指标 + 随机森林回归，对个股进行短期预测和信号生成。

依赖: numpy, pandas, scikit-learn
数据源: akshare_client.get_history() 获取的历史K线数据
"""
import logging
from typing import Optional
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split

logger = logging.getLogger(__name__)


# ==================== 技术指标计算 ====================

def ma(prices: list, window: int = 5) -> list:
    """简单移动平均线 (Moving Average)"""
    series = pd.Series(prices)
    return series.rolling(window=window, min_periods=1).mean().tolist()


def rsi(prices: list, window: int = 14) -> list:
    """相对强弱指标 (RSI)"""
    series = pd.Series(prices)
    delta = series.diff()
    gain = delta.clip(lower=0).rolling(window=window, min_periods=1).mean()
    loss = (-delta.clip(upper=0)).rolling(window=window, min_periods=1).mean()
    rs = gain / loss.replace(0, np.nan)
    rsi_val = 100 - (100 / (1 + rs))
    return rsi_val.fillna(50).tolist()


def macd(prices: list, fast: int = 12, slow: int = 26, signal: int = 9) -> dict:
    """MACD 指标 (异同移动平均线)"""
    series = pd.Series(prices)
    ema_fast = series.ewm(span=fast, adjust=False).mean()
    ema_slow = series.ewm(span=slow, adjust=False).mean()
    dif = (ema_fast - ema_slow).tolist()
    dea = pd.Series(dif).ewm(span=signal, adjust=False).mean().tolist()
    hist = [dif[i] - dea[i] for i in range(len(dif))]
    return {"dif": dif, "dea": dea, "hist": hist}


def bollinger(prices: list, window: int = 20, num_std: float = 2.0) -> dict:
    """布林带 (Bollinger Bands)"""
    series = pd.Series(prices)
    middle = series.rolling(window=window, min_periods=1).mean()
    std = series.rolling(window=window, min_periods=1).std()
    upper = (middle + num_std * std).tolist()
    mid = middle.tolist()
    lower = (middle - num_std * std).tolist()
    return {"upper": upper, "middle": mid, "lower": lower}


def generate_signal(prices: list) -> dict:
    """综合技术指标生成买卖信号

    返回:
        signal: "买入" / "卖出" / "持有" / "无法判断"
        score: -100 ~ +100 的综合打分
        details: 各指标的判断明细
    """
    if not prices or len(prices) < 20:
        return {"signal": "无法判断", "score": 0, "details": {"reason": "数据不足(需至少20个交易日)"}}

    close = np.array(prices, dtype=float)
    rsi_vals = rsi(prices)
    macd_vals = macd(prices)
    ma_short = ma(prices, 5)
    ma_long = ma(prices, 20)
    boll = bollinger(prices)

    latest = close[-1]
    score = 0.0
    details = {}

    # --- RSI 判断 ---
    rsi_now = rsi_vals[-1]
    if rsi_now < 30:
        score += 30
        details["RSI"] = f"超卖({rsi_now:.1f})，看涨"
    elif rsi_now > 70:
        score -= 30
        details["RSI"] = f"超买({rsi_now:.1f})，看跌"
    else:
        details["RSI"] = f"中性({rsi_now:.1f})"

    # --- MACD 判断 ---
    hist_now = macd_vals["hist"][-1]
    hist_prev = macd_vals["hist"][-2] if len(macd_vals["hist"]) >= 2 else 0
    if hist_now > 0 and hist_prev <= 0:
        score += 25
        details["MACD"] = "金叉，看涨"
    elif hist_now < 0 and hist_prev >= 0:
        score -= 25
        details["MACD"] = "死叉，看跌"
    elif hist_now > 0:
        score += 10
        details["MACD"] = f"多头({hist_now:.2f})"
    else:
        score -= 10
        details["MACD"] = f"空头({hist_now:.2f})"

    # --- 均线判断 ---
    if len(ma_short) >= 20 and len(ma_long) >= 20:
        if ma_short[-1] > ma_long[-1] and ma_short[-2] <= ma_long[-2]:
            score += 20
            details["MA"] = "短期均线上穿长期均线（金叉）"
        elif ma_short[-1] < ma_long[-1] and ma_short[-2] >= ma_long[-2]:
            score -= 20
            details["MA"] = "短期均线下穿长期均线（死叉）"
        elif ma_short[-1] > ma_long[-1]:
            score += 8
            details["MA"] = f"多头排列 (MA5:{ma_short[-1]:.2f} > MA20:{ma_long[-1]:.2f})"
        else:
            score -= 8
            details["MA"] = f"空头排列 (MA5:{ma_short[-1]:.2f} < MA20:{ma_long[-1]:.2f})"

    # --- 布林带判断 ---
    upper = boll["upper"][-1]
    lower = boll["lower"][-1]
    if latest <= lower:
        score += 20
        details["Bollinger"] = f"触及下轨({lower:.2f})，超卖反弹信号"
    elif latest >= upper:
        score -= 20
        details["Bollinger"] = f"触及上轨({upper:.2f})，超买回调信号"
    else:
        pos = (latest - lower) / (upper - lower) * 100
        details["Bollinger"] = f"中轨附近(位置:{pos:.0f}%)"

    # --- 综合判断 ---
    score = max(-100, min(100, score))
    if score >= 40:
        signal = "看涨 📈"
    elif score <= -40:
        signal = "看跌 📉"
    elif score >= 15:
        signal = "偏多 ↗️"
    elif score <= -15:
        signal = "偏空 ↘️"
    else:
        signal = "震荡/持有 ➡️"

    return {"signal": signal, "score": score, "details": details}


# ==================== 机器学习预测 ====================

class StockPredictor:
    """基于随机森林回归的短期价格预测"""

    def __init__(self):
        self.model = RandomForestRegressor(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1,
        )
        self.trained = False

    def _prepare_features(self, prices: list) -> tuple:
        """从价格序列构建特征矩阵"""
        df = pd.DataFrame({"close": pd.Series(prices, dtype=float)})

        # 基础特征: 过去N日收益率
        for lag in [1, 2, 3, 5, 10]:
            df[f"ret_{lag}"] = df["close"].pct_change(lag)

        # 技术指标特征
        df["ma5"] = ma(df["close"].tolist(), 5)
        df["ma20"] = ma(df["close"].tolist(), 20)
        df["rsi"] = rsi(df["close"].tolist(), 14)
        boll = bollinger(df["close"].tolist())
        df["boll_pos"] = (df["close"] - pd.Series(boll["lower"])) / (pd.Series(boll["upper"]) - pd.Series(boll["lower"])).replace(0, 1)

        # 目标: 下一日收益率
        df["target"] = df["close"].pct_change(1).shift(-1)

        # 删除NaN
        df = df.dropna()

        if len(df) < 20:
            return None, None

        feature_cols = [c for c in df.columns if c != "close" and c != "target"]
        X = df[feature_cols].values
        y = df["target"].values
        return X, y

    def train(self, prices: list) -> bool:
        """训练模型"""
        X, y = self._prepare_features(prices)
        if X is None or len(X) < 20:
            logger.warning("训练数据不足")
            return False

        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        self.model.fit(X_train, y_train)
        self.trained = True

        train_score = self.model.score(X_train, y_train)
        test_score = self.model.score(X_test, y_test)
        logger.info(f"模型训练完成 - 训练集R²: {train_score:.3f}, 测试集R²: {test_score:.3f}")
        return True

    def predict(self, prices: list) -> Optional[dict]:
        """预测下一交易日涨跌幅"""
        if not self.trained:
            # 如果未训练，自动训练
            if not self.train(prices):
                return None

        X, _ = self._prepare_features(prices)
        if X is None or len(X) == 0:
            return None

        # 使用最新一天的特征做预测
        latest_features = X[-1].reshape(1, -1)
        pred_return = self.model.predict(latest_features)[0]

        # 特征重要性
        importance = {}
        feature_names = [c for c in pd.DataFrame({"close": prices}).columns.tolist()
                         if c != "close"]
        for name, imp in zip(
            ["ret_1", "ret_2", "ret_3", "ret_5", "ret_10", "ma5", "ma20", "rsi", "boll_pos"],
            self.model.feature_importances_,
        ):
            importance[name] = float(round(imp, 4))

        return {
            "predicted_change_pct": round(float(pred_return * 100), 2),
            "confidence": float(round(self.model.score(X, np.zeros(len(X))) + 0.5, 2)),  # 近似置信度
            "feature_importance": importance,
        }


# ==================== 主分析函数 ====================


def analyze_stock(prices: list, symbol: str = "") -> dict:
    """对一只股票进行完整量化分析

    Args:
        prices: 收盘价列表（按日期从旧到新）
        symbol: 股票代码（可选，仅用于日志）

    Returns:
        包含技术信号、预测、统计信息的字典
    """
    if not prices or len(prices) < 5:
        return {"symbol": symbol, "error": "数据不足"}

    close = [float(p) for p in prices]

    # --- 基础统计 ---
    latest = close[-1]
    prev = close[-2] if len(close) >= 2 else close[-1]
    change_pct = ((latest - prev) / prev) * 100
    high = max(close)
    low = min(close)
    avg_price = np.mean(close)
    volatility = np.std(close) / avg_price * 100 if avg_price > 0 else 0

    stats = {
        "latest_price": round(latest, 2),
        "change_pct": round(change_pct, 2),
        "high_20d": round(high, 2),
        "low_20d": round(low, 2),
        "avg_20d": round(avg_price, 2),
        "volatility_pct": round(volatility, 2),
    }

    # --- 技术信号 ---
    tech_signal = generate_signal(close)
    current_rsi = rsi(close)[-1]
    current_macd = macd(close)
    current_boll = bollinger(close)

    indicators = {
        "ma5": round(ma(close, 5)[-1], 2),
        "ma20": round(ma(close, 20)[-1], 2) if len(close) >= 20 else None,
        "rsi": round(current_rsi, 1),
        "macd": {
            "dif": round(current_macd["dif"][-1], 3),
            "dea": round(current_macd["dea"][-1], 3),
            "hist": round(current_macd["hist"][-1], 3),
        },
        "bollinger": {
            "upper": round(current_boll["upper"][-1], 2),
            "middle": round(current_boll["middle"][-1], 2),
            "lower": round(current_boll["lower"][-1], 2),
        },
    }

    # --- ML 预测 ---
    predictor = StockPredictor()
    prediction = predictor.predict(close)

    result = {
        "symbol": symbol,
        "stats": stats,
        "signal": tech_signal,
        "indicators": indicators,
        "prediction": prediction,
    }

    return result
