<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStock, getFavorites, addFavorite, deleteFavorite } from '../api/stock.js'

const router = useRouter()
const symbol = ref('')
const stockData = ref(null)
const favorites = ref([])
const loading = ref(false)
const error = ref('')

const search = async () => {
  if (!symbol.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await getStock(symbol.value.toUpperCase())
    stockData.value = res.data
  } catch (e) {
    error.value = '查询失败，请检查股票代码'
    stockData.value = null
  } finally {
    loading.value = false
  }
}

const loadFavorites = async () => {
  try {
    const res = await getFavorites()
    favorites.value = res.data
  } catch (e) { /* ignore */ }
}

const add = async (sym) => {
  try {
    await addFavorite(sym)
    await loadFavorites()
  } catch (e) { /* ignore */ }
}

const remove = async (sym) => {
  try {
    await deleteFavorite(sym)
    await loadFavorites()
  } catch (e) { /* ignore */ }
}

const goDetail = (sym) => router.push(`/stock/${sym}`)

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(loadFavorites)
</script>

<template>
  <div class="app-layout">
    <header>
      <h1>Stock Tracker</h1>
      <button class="logout-btn" @click="logout">退出</button>
    </header>
    <main>
      <section class="search-section">
        <input v-model="symbol" @keyup.enter="search" placeholder="输入股票代码，如 AAPL" />
        <button @click="search" :disabled="loading">{{ loading ? '查询中...' : '查询' }}</button>
      </section>

      <p v-if="error" class="error">{{ error }}</p>

      <div v-if="stockData" class="stock-card" @click="goDetail(symbol.toUpperCase())">
        <div class="price-main">
          <span class="symbol">{{ symbol.toUpperCase() }}</span>
          <span class="price">${{ stockData.price }}</span>
        </div>
        <p class="update-time">更新: {{ stockData.lastUpdated || 'N/A' }}</p>
        <button class="fav-btn" @click.stop="add(symbol.toUpperCase())">+ 添加自选</button>
      </div>

      <section class="favorites">
        <h2>自选股</h2>
        <div v-if="favorites.length === 0" class="empty">暂无自选股</div>
        <div v-for="item in favorites" :key="item.symbol" class="fav-item" @click="goDetail(item.symbol)">
          <div>
            <strong>{{ item.symbol }}</strong>
            <span class="fav-price">${{ item.price }}</span>
          </div>
          <button class="del-btn" @click.stop="remove(item.symbol)">删除</button>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.app-layout { min-height: 100vh; background: #f0f2f5; }
header { background: #1a1a2e; color: white; padding: 16px 24px; display: flex; justify-content: space-between; align-items: center; }
header h1 { margin: 0; font-size: 20px; }
.logout-btn { background: transparent; border: 1px solid white; color: white; padding: 6px 16px; border-radius: 4px; cursor: pointer; }
main { max-width: 640px; margin: 0 auto; padding: 24px 16px; }
.search-section { display: flex; gap: 8px; margin-bottom: 20px; }
.search-section input { flex: 1; padding: 10px 12px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; }
.search-section button { padding: 10px 20px; background: #1677ff; color: white; border: none; border-radius: 4px; cursor: pointer; }
.stock-card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.1); cursor: pointer; }
.price-main { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8px; }
.symbol { font-size: 24px; font-weight: bold; }
.price { font-size: 28px; font-weight: bold; color: #52c41a; }
.update-time { color: #888; font-size: 13px; margin-bottom: 12px; }
.fav-btn { padding: 6px 16px; background: #52c41a; color: white; border: none; border-radius: 4px; cursor: pointer; }
.favorites h2 { font-size: 18px; margin-bottom: 12px; }
.empty { color: #999; text-align: center; padding: 32px; }
.fav-item { background: white; border-radius: 6px; padding: 12px 16px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 4px rgba(0,0,0,0.08); cursor: pointer; }
.fav-price { margin-left: 12px; color: #52c41a; font-weight: bold; }
.del-btn { padding: 4px 12px; background: #ff4d4f; color: white; border: none; border-radius: 4px; cursor: pointer; }
.error { color: #ff4d4f; margin-bottom: 12px; }
</style>
