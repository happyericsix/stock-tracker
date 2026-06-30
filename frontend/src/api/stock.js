import request from './request.js'

export const getStock = (symbol) => request.get(`/stocks/${symbol}`)
export const getOverview = (symbol) => request.get(`/stocks/${symbol}/overview`)
export const getHistory = (symbol, page = 0, size = 100) =>
  request.get(`/stocks/${symbol}/history`, { params: { page, size } })

export const getFavorites = () => request.get('/stocks/favorites')
export const addFavorite = (symbol) => request.post('/stocks/favorites', { symbol })
export const deleteFavorite = (symbol) => request.delete(`/stocks/favorites/${symbol}`)
