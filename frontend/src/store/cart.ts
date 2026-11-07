import { defineStore } from 'pinia'
import { cartList, cartMerge } from '@/api'
import { LOCAL_CART_KEY } from '@/utils/const'
import { useUserStore } from './user'

/** 本地离线购物车条目 */
export interface LocalCartEntry {
  skuId: string
  quantity: number
}

/**
 * 购物车：游客离线缓存（localStorage）与登录后服务端购物车的合并。
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    /** 服务端购物车 [{id, skuId, title, image, specInfo, price, quantity, checked, invalid, ...}] */
    server: [] as any[],
    /** 游客本地 [{skuId, quantity}] */
    local: readLocal(),
    count: 0
  }),
  getters: {
    isOnline: (): boolean => useUserStore().isLogin
  },
  actions: {
    /** 游客本地加购（离线可用） */
    localAdd(skuId: string, quantity = 1) {
      const it = this.local.find((x) => x.skuId === skuId)
      if (it) it.quantity += quantity
      else this.local.push({ skuId, quantity })
      saveLocal(this.local)
    },
    localRemove(skuId: string) {
      this.local = this.local.filter((x) => x.skuId !== skuId)
      saveLocal(this.local)
    },
    /** 登录后：服务端购物车 + 本地合并入库 */
    async syncAfterLogin() {
      if (this.local.length) {
        try {
          const res: any = await cartMerge(this.local)
          this.count = res.cartCount || 0
        } catch (e) {
          /* 合并失败不阻塞登录 */
        }
        this.local = []
        saveLocal(this.local)
      }
      await this.load()
    },
    async load() {
      if (!useUserStore().isLogin) {
        this.server = []
        this.count = 0
        return
      }
      try {
        const res: any = await cartList()
        this.server = res.items || []
        this.count = (res.totalQuantity as number) || 0
        this.computeCount()
      } catch (e) {
        /* 401 等静默 */
      }
    },
    /** 服务端条目勾选/数量变化后重算角标 */
    computeCount() {
      this.count = this.server.filter((i) => i.checked).reduce((s, i) => s + i.quantity, 0)
    },
    /** 结算后本地数量更新（服务端返回 cartCount 时） */
    setCount(n: number) {
      this.count = n
    }
  }
})

function readLocal(): LocalCartEntry[] {
  try {
    return JSON.parse(localStorage.getItem(LOCAL_CART_KEY) || '[]')
  } catch (e) {
    return []
  }
}

function saveLocal(list: LocalCartEntry[]) {
  localStorage.setItem(LOCAL_CART_KEY, JSON.stringify(list))
}