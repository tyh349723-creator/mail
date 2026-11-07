<template>
  <div class="page">
    <!-- 用户信息 -->
    <div class="user-card">
      <img class="avatar" :src="user.avatar || 'https://picsum.photos/seed/avatar/200/200'" />
      <div class="uinfo">
        <template v-if="user.isLogin">
          <div class="nick">{{ user.nickname }}<span class="num">{{ user.userNum }}</span></div>
          <div class="sub">演示用户</div>
        </template>
        <template v-else>
          <div class="nick">未登录</div>
          <div class="sub">登录后可同步购物车 / 下单购物</div>
        </template>
      </div>
      <button v-if="!user.isLogin" class="login-btn" @click="$router.push('/')">登录</button>
    </div>

    <div class="ent-grid" v-if="user.isLogin">
      <div class="ent" @click="$router.push('/user/orders')"><span class="ic"><img src="@/assets/order.png" style="width:26px;height:26px;margin-left:46px;margin-top:5px" /></span>我的订单</div>
      <div class="ent" @click="$router.push('/user/cart')"><span class="ic"><img src="@/assets/a1.png" style="width:28px;height:28px;margin-left:40px;margin-top:2px" /></span>购物车</div>
      <div class="ent" @click="$router.push('/user/address')"><span class="ic"><img src="@/assets/address.png" style="width:28px;height:28px;margin-left:43px;margin-top:2px" /></span>收货地址</div>
      <div class="ent" @click="editProfile"><span class="ic"><span class="ic"><img src="@/assets/profile.png" style="width:28px;height:28px;margin-left:43px;margin-top:2px" /></span></span>编辑资料</div>
    </div>

    <div class="card menu">
      <div class="cell" @click="switchMode"><span class="ic"><img src="@/assets/shangjia.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>商家入驻 / 商家工作台<span class="arrow">›</span></div>
      <div class="cell" @click="about"><span class="ic"><img src="@/assets/system.png" style="width:28px;height:28px;margin-left:0px;margin-top:2px" /></span>关于系统<span class="arrow">›</span></div>
      <div v-if="user.isLogin" class="cell logout" @click="logout">退出登录</div>
    </div>
    <Tabbar :active="3" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import Tabbar from '@/components/mobile/Tabbar.vue'
import { updateProfile } from '@/api'

const router = useRouter()
const user = useUserStore()
const cart = useCartStore()

function switchMode() {
  // 商家调用商户端需切换账号：登出后回门户并预选“商户”角色
  user.logout()
  cart.local = []
  cart.server = []
  router.push({ path: '/', query: { role: 'mch' } })
}
function logout() {
  user.logout()
  cart.local = []
  cart.server = []
  router.replace('/')
}
async function editProfile() {
  const nick = prompt('昵称：', user.nickname)
  if (!nick) return
  await updateProfile({ nickname: nick })
  user.nickname = nick
  localStorage.setItem('mall_nickname', nick)
}
function about() {
  alert('多商家电商平台 Mall-X\n用户端 / 商家端 双模式演示\n后端：网关 10010 + 业务 10011 + 支付 10012')
}
</script>

<style scoped>
.user-card { background: linear-gradient(135deg, #ff8a5c, #ff3e3e); padding: 26px 20px; display: flex; align-items: center; gap: 14px; color: #fff; }
.avatar { width: 64px; height: 64px; border-radius: 50%; border: 2px solid rgba(255,255,255,.7); }
.nick { font-size: 18px; font-weight: 700; }
.num { font-size: 12px; font-weight: 400; opacity: .8; margin-left: 8px; }
.sub { font-size: 12px; opacity: .9; margin-top: 4px; }
.login-btn { margin-left: auto; padding: 8px 22px; border-radius: 18px; background: rgba(255,255,255,.9); color: var(--primary); font-size: 14px; }
.ent-grid { display: grid; grid-template-columns: repeat(4, 1fr); background: #fff; margin: 10px; border-radius: 12px; padding: 16px 0; }
.ent { text-align: center; font-size: 12px; color: #333; }
.ic { display: block; font-size: 22px; margin-bottom: 6px; }
.menu { margin: 10px; }
.menu .cell .ic { margin-right: 8px; }
.logout { color: #ff3e3e; justify-content: center; border: none; }
</style>