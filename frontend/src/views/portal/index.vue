<template>
  <div class="portal-page">
    <div class="auth-card">
      <div class="brand">
        <img src="/favicon.svg" class="brand-logo" />
        <h1 class="brand-title">Mall-X 多商家电商平台</h1>
        <p class="brand-sub">选择角色登录 / 注册</p>
      </div>

      <el-form :model="form" label-position="top" size="large" @keyup.enter="submit">
        <el-form-item label="选择角色">
          <el-select v-model="activeRole" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.key" :label="r.name" :value="r.key" />
          </el-select>
        </el-form-item>

        <div class="mode-bar">
          <el-radio-group v-model="mode" size="small">
            <el-radio-button value="login">登录</el-radio-button>
            <el-radio-button v-if="activeRole !== 'admin'" value="register">注册</el-radio-button>
          </el-radio-group>
        </div>

        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" clearable />
        </el-form-item>

        <!-- 注册附加字段：用户 -->
        <template v-if="mode === 'register' && activeRole === 'user'">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="昵称（可选）" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="手机号（可选）" />
          </el-form-item>
        </template>

        <!-- 注册附加字段：商户（注册即提交入驻资料，平台审核通过后方可经营） -->
        <template v-else-if="mode === 'register' && activeRole === 'mch'">
          <el-form-item label="店铺名称">
            <el-input v-model="form.shopName" placeholder="店铺名称" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.shopPhone" placeholder="联系电话" />
          </el-form-item>
          <el-form-item label="经营地址">
            <el-input v-model="form.shopAddress" placeholder="经营地址" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="联系人姓名（可选）" />
          </el-form-item>
        </template>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="确认密码">
          <el-input v-model="form.password2" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            {{ submitText }}
          </el-button>
        </el-form-item>

        <!-- 注册模式：随时可返回登录（登录页即本首页） -->
        <div v-if="mode === 'register'" class="auth-switch">
          <span class="auth-switch-text">已有账号？</span>
          <el-button link type="primary" @click="goLogin">去登录</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore, roleHome, type Role } from '@/store/user'
import { userRegister, mchRegister } from '@/api/portal'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const roles: Array<{ key: Role; name: string }> = [
  { key: 'user', name: '用户' },
  { key: 'mch', name: '商户' },
  { key: 'admin', name: '管理员' }
]

// 支持外部带 ?role= 预选角色（如移动端「商家入口 / 返回门户」跳回时携带）
const ROLE_KEYS: Role[] = ['user', 'mch', 'admin']
const qRole = String(route.query.role || '')
const activeRole = ref<Role>(ROLE_KEYS.includes(qRole as Role) ? (qRole as Role) : 'user')
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  password2: '',
  nickname: '',
  phone: '',
  shopName: '',
  shopPhone: '',
  shopAddress: '',
  contactName: ''
})

const submitText = computed(() => {
  if (activeRole.value === 'admin') return '登 录'
  return mode.value === 'register' ? '注 册' : '登 录'
})

function validate(): string {
  if (!form.username || !form.password) return '请输入账号与密码'
  if (mode.value === 'register') {
    if (form.password !== form.password2) return '两次输入的密码不一致'
    if (activeRole.value === 'mch' && (!form.shopName || !form.shopPhone || !form.shopAddress)) {
      return '请完整填写店铺名称、联系电话与经营地址'
    }
  }
  return ''
}

/** 注册模式 → 返回登录表单（保留账号待登录，清空密码与注册附加字段） */
function goLogin() {
  mode.value = 'login'
  form.password = ''
  form.password2 = ''
}

/** 注册成功后回到登录注册首页（即本页登录表单），账号自动带入 */
function backToLoginForm() {
  goLogin()
  form.nickname = ''
  form.phone = ''
  form.shopName = ''
  form.shopPhone = ''
  form.shopAddress = ''
  form.contactName = ''
}

async function submit() {
  const err = validate()
  if (err) {
    ElMessage.warning(err)
    return
  }

  loading.value = true
  try {
    // 管理员：直接进入管理后台
    if (activeRole.value === 'admin') {
      await userStore.login(form.username, form.password)
      ElMessage.success('登录成功，欢迎回来')
      router.push('/admin/dashboard')
      return
    }

    // 用户/商户：注册成功后返回登录注册首页，用新账号二次登录
    if (mode.value === 'register') {
      if (activeRole.value === 'user') {
        await userRegister({
          username: form.username,
          password: form.password,
          nickname: form.nickname,
          phone: form.phone
        })
      } else {
        await mchRegister({
          username: form.username,
          password: form.password,
          shopName: form.shopName,
          shopPhone: form.shopPhone,
          shopAddress: form.shopAddress,
          contactName: form.contactName
        })
      }
      ElMessage.success(activeRole.value === 'mch'
        ? '注册成功，入驻资料已提交平台审核，登录后可查看审核进度'
        : '注册成功，请使用新账号登录')
      backToLoginForm()
      return
    }

    // 用户/商户：登录后按角色进入对应工作台（同域单页跳转，无需跨端口/URL 传 token）
    if (activeRole.value === 'user') {
      await userStore.loginUser(form.username, form.password)
    } else {
      await userStore.loginMch(form.username, form.password)
    }
    ElMessage.success('登录成功，欢迎回来')
    router.push(roleHome(activeRole.value, userStore.mchState))
  } catch (e) {
    // 业务错误提示已由 request.ts 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.portal-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #0f1f3d 0%, #1c3d6e 55%, #2a5298 100%);
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 14px;
  padding: 30px 32px 24px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.3);

  .brand {
    text-align: center;
    margin-bottom: 20px;

    .brand-logo {
      width: 46px;
      height: 46px;
    }
    .brand-title {
      margin-top: 10px;
      font-size: 20px;
      font-weight: 700;
      color: #303133;
    }
    .brand-sub {
      margin-top: 4px;
      font-size: 13px;
      color: #909399;
    }
  }

  .mode-bar {
    margin-bottom: 16px;
  }

  .submit-btn {
    width: 100%;
  }

  .auth-switch {
    margin-top: 12px;
    text-align: center;
    font-size: 13px;

    .auth-switch-text {
      color: #909399;
    }
  }
}
</style>