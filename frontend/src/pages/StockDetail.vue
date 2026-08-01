<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStock, getOverview, getHistory } from '../api/stock.js'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const stock = ref(null)
const overview = ref(null)
const error = ref('')
const chartRef = ref(null)

let chart = null

const handleResize = () => chart?.resize()

const disposeChart = () => {
  if (chart) {
    chart.dispose()
    chart = null
  }
  window.removeEventListener('resize', handleResize)
}

const loadData = async (sym) => {
  error.value = ''
  stock.value = null
  overview.value = null
  disposeChart()

  const [stockRes, overviewRes, historyRes] = await Promise.all([
    getStock(sym).catch(() => null),
    getOverview(sym).catch(() => null),
    getHistory(sym, 0, 100).catch(() => null)
  ])

  stock.value = stockRes?.data ?? null
  overview.value = overviewRes?.data ?? null

  // 三个 API 全部失败时显示错误
  if (!stockRes?.data && !overviewRes?.data) {
    error.value = '数据加载失败，请检查股票代码或稍后重试'
    return
  }

  if (historyRes?.data?.content?.length) {
    await nextTick()
    if (!chartRef.value) return
    chart = echarts.init(chartRef.value)
    const data = [...historyRes.data.content].reverse()
    chart.setOption({
      title: { text: `${sym} 历史走势`, left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: data.map(d => d.date), axisLabel: { rotate: 45 } },
      yAxis: { type: 'value' },
      series: [{
        type: 'line',
        data: data.map(d => d.close),
        smooth: true,
        lineStyle: { color: '#1677ff', width: 2 },
        areaStyle: { color: 'rgba(22,119,255,0.1)' }
      }]
    })
    window.addEventListener('resize', handleResize)
  }
}

onMounted(() => loadData(route.params.symbol))
onUnmounted(disposeChart)

watch(() => route.params.symbol, (newSym) => {
  if (newSym) loadData(newSym)
})
</script>

<template>
  <div class="detail-page">
    <header>
      <button class="back" @click="router.back()">← 返回</button>
      <h1>{{ route.params.symbol }}</h1>
    </header>
    <main>
      <p v-if="error" class="error-msg">{{ error }}</p>
      <div v-if="!stock && !overview && !error" class="empty-state">加载中...</div>
      <div v-if="stock" class="price-card">
        <span class="price">${{ stock.price || "N/A" }}</span>
        <span class="update">更新: {{ stock.lastUpdated || 'N/A' }}</span>
      </div>
      <div v-if="overview" class="overview">
        <h2>公司概况</h2>
        <p><strong>名称:</strong> {{ overview.name }}</p>
        <p><strong>行业:</strong> {{ overview.sector }} / {{ overview.industry }}</p>
        <p><strong>市值:</strong> ${{ overview.marketCapitalization }}</p>
        <p><strong>市盈率:</strong> {{ overview.peRatio }}</p>
        <p><strong>股息率:</strong> {{ overview.dividendYield }}</p>
        <p class="desc">{{ overview.description }}</p>
      </div>
      <div ref="chartRef" class="chart"></div>
    </main>
  </div>
</template>

<style scoped>
.detail-page { min-height: 100vh; background: #f0f2f5; }
header { background: #1a1a2e; color: white; padding: 16px 24px; display: flex; align-items: center; gap: 16px; }
header h1 { margin: 0; font-size: 20px; }
.back { background: transparent; border: 1px solid white; color: white; padding: 6px 16px; border-radius: 4px; cursor: pointer; }
main { max-width: 800px; margin: 0 auto; padding: 24px 16px; }
.price-card { background: white; border-radius: 8px; padding: 24px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.price { font-size: 36px; font-weight: bold; color: #52c41a; display: block; margin-bottom: 8px; }
.update { color: #888; font-size: 13px; }
.overview { background: white; border-radius: 8px; padding: 24px; margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.overview h2 { margin-bottom: 16px; }
.overview p { margin-bottom: 8px; font-size: 14px; }
.desc { margin-top: 12px; color: #555; line-height: 1.6; }
.chart { background: white; border-radius: 8px; padding: 16px; height: 400px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); }
.error-msg { color: #ff4d4f; text-align: center; padding: 24px; font-size: 14px; }
.empty-state { color: #999; text-align: center; padding: 48px 24px; font-size: 14px; }
</style>