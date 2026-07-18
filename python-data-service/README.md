# Stock Data Service (akshare)

为 stock-tracker Java 后端提供数据服务的 Python 微服务，基于 [akshare](https://github.com/akfamily/akshare) 获取 A 股/港股/美股行情数据。

## 快速启动

`ash
# 1. 创建虚拟环境（推荐）
python -m venv venv
venv\Scripts\activate

# 2. 安装依赖
pip install -r requirements.txt

# 3. 启动服务（热重载）
uvicorn app:app --reload --host 0.0.0.0 --port 8000
`

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /health | 健康检查 |
| GET | /api/v1/quote/{symbol} | 实时行情 |
| GET | /api/v1/history/{symbol} | K 线历史 |
| GET | /api/v1/overview/{symbol} | 基本面概况 |

## 与 Java 后端集成

在 StockService.java 中新增 AkshareStockClient，通过 WebClient 调用 http://python-data-service:8000/api/v1/...，替换原有的 ChoiceStockClient。

### docker-compose 示例

`yaml
python-data:
  build: ./python-data-service
  container_name: stock-data
  ports:
    - "8000:8000"
`

## 数据说明

- A 股代码直接传数字即可，如 600519（无需 .SH 后缀）
- 历史 K 线默认前复权（djust="qfq"）
- akShare 数据来源于东方财富等公开财经网站，仅供学习参考
