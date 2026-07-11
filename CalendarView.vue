<template>
  <div class="calendar-container">
    <!-- 背景层 -->
    <div class="bg-layer"></div>

    <div class="layout-wrapper">
      <!-- 1. 左侧全局导航栏 -->
      <div class="left-sidebar">
        <div class="logo-wrapper">
          <img src="@/assets/logo.png" alt="友趣 Logo" class="app-logo" />
        </div>
        <div class="nav-links">
          <button @click="goToFriends" class="nav-btn">👥</button>
          <button @click="goToProfile" class="nav-btn">👤</button>
          <button @click="goToCalendar" class="nav-btn active">📅</button>
          <button @click="goToChat" class="nav-btn">💬</button>
          <button @click="$router.push('/home')" class="nav-btn">📋</button>
        </div>
      </div>

      <!-- 2. 右侧主内容区 -->
      <div class="main-content">
        <div class="content-grid">

          <!-- ===== 左半部分：缩小后的日历展示区 ===== -->
          <div class="calendar-card">
            <!-- 月份切换 -->
            <div class="month-nav">
              <button @click="prevMonth" class="nav-btn-cal">‹</button>
              <span class="month-title">{{ currentYear }}年 {{ currentMonth }}月</span>
              <button @click="nextMonth" class="nav-btn-cal">›</button>
            </div>

            <!-- 日历网格 -->
            <div class="weekdays">
              <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
            </div>

            <div class="calendar-grid">
              <div 
                v-for="(day, index) in calendarDays" 
                :key="index"
                class="calendar-day"
                :class="{
                  'other-month': !day.isCurrentMonth,
                  'today': day.isToday,
                  'has-event': day.hasEvent
                }"
              >
                <span class="day-number">{{ day.day }}</span>
                <span v-if="day.hasEvent" class="event-dot">●</span>
              </div>
            </div>
          </div>

          <!-- ===== 右半部分：功能全面的查询列表 ===== -->
          <div class="event-panel">
            
            <!-- 上窄条：筛选操作栏 (恢复原版所有功能) -->
            <div class="event-tabs-bar">
              <div class="filter-options">
                <button 
                  v-for="filter in filterOptions" 
                  :key="filter.value"
                  @click="applyFilter(filter.value)"
                  :class="['filter-btn', { active: currentFilter === filter.value }]"
                >
                  {{ filter.label }}
                </button>
              </div>
              
              <!-- ✅ 日期范围选择器 (当选中 'dateRange' 时出现) -->
              <div class="date-range-filter" v-if="currentFilter === 'dateRange'">
                <input type="date" v-model="dateRangeStart" @change="applyDateRangeFilter" />
                <span>至</span>
                <input type="date" v-model="dateRangeEnd" @change="applyDateRangeFilter" />
                <button @click="clearDateRange" class="clear-btn">✕</button>
              </div>

              <!-- 添加按钮靠右 -->
              <button @click="showAddEvent = true" class="add-event-btn">+ 添加</button>
            </div>

            <!-- 下大框：显示内容列表 (透明底) -->
            <div class="event-content-box">
              <div v-if="filteredEvents.length === 0" class="empty-state">暂无活动</div>
              <div v-else v-for="(event, index) in filteredEvents" :key="event.id" class="event-item">
                <!-- 左侧数字序号 -->
                <span class="event-index">{{ index + 1 }}</span>
                <!-- 活动内容 -->
                <div class="event-info" @click="showEventDetailModal(event)">
                  <div class="event-title">{{ event.title }}</div>
                  <div class="event-desc">{{ event.description || formatDateShort(event.date) }}</div>
                  <div class="event-status" :class="getStatusClass(event.status)">
                    {{ getStatusText(event.status) }}
                  </div>
                </div>
                <!-- 操作按钮 -->
                <div class="event-actions">
                  <button @click="editEvent(event)" class="edit-btn">✏️</button>
                  <button @click="deleteEvent(event.id)" class="del-btn">✕</button>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>

    <!-- ===== 添加/编辑活动弹窗 ===== -->
    <div v-if="showAddEvent || showEditEvent" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h2>{{ showEditEvent ? '编辑活动' : '添加活动' }}</h2>
        <div class="form-group">
          <label>活动标题</label>
          <input v-model="eventForm.title" placeholder="请输入活动标题" />
        </div>
        <div class="form-group">
          <label>日期</label>
          <input type="date" v-model="eventForm.date" />
        </div>
        <div class="form-group">
          <label>时间（可选）</label>
          <input type="time" v-model="eventForm.time" />
        </div>
        <div class="form-group">
          <label>描述（可选）</label>
          <textarea v-model="eventForm.description" placeholder="请输入活动描述" rows="2"></textarea>
        </div>
        <div class="form-group" v-if="showEditEvent">
          <label>状态</label>
          <select v-model="eventForm.status">
            <option :value="0">待进行</option>
            <option :value="1">进行中</option>
            <option :value="2">已完成</option>
            <option :value="3">已取消</option>
          </select>
        </div>
        <div class="modal-actions">
          <button @click="closeModal" class="cancel-btn">取消</button>
          <button @click="saveEvent" class="save-btn">保存</button>
        </div>
      </div>
    </div>

    <!-- ===== 活动详情弹窗 ===== -->
    <div v-if="showEventDetail" class="modal-overlay" @click.self="showEventDetail = false">
      <div class="modal">
        <h2>📋 活动详情</h2>
        <div class="event-detail-item"><strong>标题：</strong>{{ selectedEventDetail?.title }}</div>
        <div class="event-detail-item"><strong>日期：</strong>{{ selectedEventDetail?.date }}</div>
        <div class="event-detail-item" v-if="selectedEventDetail?.time"><strong>时间：</strong>{{ selectedEventDetail?.time }}</div>
        <div class="event-detail-item" v-if="selectedEventDetail?.description"><strong>描述：</strong>{{ selectedEventDetail?.description }}</div>
        <div class="event-detail-item"><strong>状态：</strong>{{ getStatusText(selectedEventDetail?.status) }}</div>
        <div class="modal-actions">
          <button @click="showEventDetail = false" class="save-btn">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

// ===== 全局导航 =====
const goToFriends = () => router.push('/friends')
const goToProfile = () => router.push('/profile')
const goToCalendar = () => router.push('/calendar')
const goToChat = () => router.push('/chat')

// ===== 日历数据 =====
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const events = ref([])
const weekdays = ['日', '一', '二', '三', '四', '五', '六']

// ===== 筛选 (恢复原版逻辑) =====
const currentFilter = ref('all')
const filterOptions = [
  { value: 'all', label: '全部' },
  { value: 'completed', label: '已完成' },
  { value: 'uncompleted', label: '未完成' },
  { value: 'dateRange', label: '按日期查询' }
]
const dateRangeStart = ref('')
const dateRangeEnd = ref('')

// ===== 弹窗 =====
const showAddEvent = ref(false)
const showEditEvent = ref(false)
const showEventDetail = ref(false)
const selectedEventDetail = ref(null)

// ===== 活动表单 =====
const eventForm = ref({
  id: null, title: '', date: new Date().toISOString().split('T')[0],
  time: '', description: '', status: 0
})

// ===== 日历计算 =====
const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value
  const firstDay = new Date(year, month - 1, 1).getDay()
  const daysInMonth = new Date(year, month, 0).getDate()
  const daysInPrevMonth = new Date(year, month - 1, 0).getDate()
  const today = new Date()
  const todayStr = today.toISOString().split('T')[0]

  const days = []
  for (let i = 0; i < 42; i++) {
    let day, isCurrentMonth, dateStr
    if (i < firstDay) {
      day = daysInPrevMonth - firstDay + i + 1
      isCurrentMonth = false
      dateStr = `${year}-${String(month - 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    } else if (i >= firstDay + daysInMonth) {
      day = i - firstDay - daysInMonth + 1
      isCurrentMonth = false
      dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    } else {
      day = i - firstDay + 1
      isCurrentMonth = true
      dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    }

    const hasEvent = events.value.some(e => e.date === dateStr)
    const isToday = dateStr === todayStr
    days.push({ day, isCurrentMonth, dateStr, hasEvent, isToday })
  }
  return days
})

// ===== 筛选后的活动列表 =====
const filteredEvents = computed(() => {
  let result = [...events.value]
  if (currentFilter.value === 'completed') result = result.filter(e => e.status === 2)
  else if (currentFilter.value === 'uncompleted') result = result.filter(e => e.status !== 2 && e.status !== 3)
  else if (currentFilter.value === 'dateRange' && dateRangeStart.value && dateRangeEnd.value) {
    result = result.filter(e => e.date >= dateRangeStart.value && e.date <= dateRangeEnd.value)
  }
  return result
})

// ===== 加载所有日程 =====
const loadEvents = async () => {
  try {
    const res = await request.get('/api/schedules')
    if (res.data.code === 0) {
      events.value = (res.data.data || []).map(item => ({
        id: item.id, title: item.title,
        date: item.startTime ? item.startTime.split('T')[0] : '',
        time: item.startTime ? item.startTime.split('T')[1]?.slice(0, 5) : '',
        description: item.description || '', status: item.status !== undefined ? item.status : 0
      }))
    }
  } catch (error) { console.error('加载日程失败:', error) }
}

// ===== 应用筛选 =====
const applyFilter = (filterValue) => {
  currentFilter.value = filterValue
  if (filterValue !== 'dateRange') {
    dateRangeStart.value = ''
    dateRangeEnd.value = ''
  }
}

// ===== 按日期范围查询 =====
const applyDateRangeFilter = async () => {
  if (!dateRangeStart.value || !dateRangeEnd.value) {
    alert('请选择完整的日期范围')
    return
  }
  currentFilter.value = 'dateRange'
  await loadEvents()
}

// ===== 清除日期范围 =====
const clearDateRange = () => {
  dateRangeStart.value = ''
  dateRangeEnd.value = ''
  currentFilter.value = 'all'
  loadEvents()
}

// ===== 月份切换 =====
const prevMonth = () => { if (currentMonth.value === 1) { currentMonth.value = 12; currentYear.value-- } else { currentMonth.value-- } }
const nextMonth = () => { if (currentMonth.value === 12) { currentMonth.value = 1; currentYear.value++ } else { currentMonth.value++ } }

// ===== 状态文本和样式 =====
const getStatusText = (status) => { const map = { 0: '待进行', 1: '进行中', 2: '已完成', 3: '已取消' }; return map[status] || '待进行' }
const getStatusClass = (status) => { const map = { 0: 'status-pending', 1: 'status-progress', 2: 'status-done', 3: 'status-canceled' }; return map[status] || '' }

// ===== 保存活动 =====
const saveEvent = async () => {
  if (!eventForm.value.title.trim()) { alert('请输入活动标题'); return }
  if (!eventForm.value.date) { alert('请选择日期'); return }
  const startDateTime = `${eventForm.value.date}T${eventForm.value.time || '00:00'}:00`
  const endDateTime = `${eventForm.value.date}T${eventForm.value.time ? String(Number(eventForm.value.time.split(':')[0]) + 1).padStart(2, '0') + ':' + eventForm.value.time.split(':')[1] : '01:00'}:00`
  const payload = { title: eventForm.value.title, description: eventForm.value.description || '', startTime: startDateTime, endTime: endDateTime, location: '', isAllDay: eventForm.value.time ? 0 : 1, remindMinutes: 30, participantIds: [], status: eventForm.value.status || 0 }
  try {
    let res
    if (showEditEvent.value && eventForm.value.id) { res = await request.put(`/api/schedules/${eventForm.value.id}`, payload) }
    else { res = await request.post('/api/schedules', payload) }
    if (res.data.code === 0) { await loadEvents(); closeModal(); alert(showEditEvent.value ? '活动更新成功！' : '活动添加成功！') }
    else { alert(res.data.message || '操作失败') }
  } catch (error) { alert('保存失败，请重试') }
}

// ===== 删除活动 =====
const deleteEvent = async (eventId) => {
  if (!confirm('确定要删除这个活动吗？')) return
  try {
    const res = await request.delete(`/api/schedules/${eventId}`)
    if (res.data.code === 0) { await loadEvents(); alert('活动已删除') } else { alert(res.data.message || '删除失败') }
  } catch (error) { alert('删除失败，请重试') }
}

// ===== 编辑与弹窗 =====
const editEvent = (event) => {
  eventForm.value = { id: event.id, title: event.title, date: event.date, time: event.time || '', description: event.description || '', status: event.status !== undefined ? event.status : 0 }
  showEditEvent.value = true
}
const closeModal = () => { showAddEvent.value = false; showEditEvent.value = false; eventForm.value = { id: null, title: '', date: new Date().toISOString().split('T')[0], time: '', description: '', status: 0 } }
const showEventDetailModal = (event) => { selectedEventDetail.value = event; showEventDetail.value = true }

// ===== 格式化日期 =====
const formatDateShort = (dateStr) => { if (!dateStr) return ''; const parts = dateStr.split('-'); return `${parts[0]}年${parseInt(parts[1])}月${parseInt(parts[2])}日` }

// ===== 初始化 =====
onMounted(() => { loadEvents() })
</script>

<style>
body { margin: 0; padding: 0; }
</style>

<style scoped>
/* ============================================================ */
/* 1. 底层布局与背景 */
.calendar-container {
  width: 100vw; height: 100vh; position: relative; margin: 0; padding: 0; overflow: hidden;
}
.bg-layer {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background-image: url('@/assets/profile-bg.png');
  background-size: cover; background-position: center;
  z-index: 1;
}
.layout-wrapper { width: 100%; height: 100%; position: absolute; top: 0; left: 0; z-index: 2; }

/* ============================================================ */
/* 2. 全局侧边栏 */
.left-sidebar {
  position: absolute; top: 0; left: 0; width: 100px; height: 100vh;
  background: rgba(35, 63, 47, 0.64); z-index: 10;
  display: flex; flex-direction: column; align-items: center; padding-top: 18px;
}
.logo-wrapper {
  width: 50px; height: 50px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 40px; flex-shrink: 0;
}
.app-logo {
  width: 100%; height: 100%;
  object-fit: contain; display: block;
}
.nav-links { display: flex; flex-direction: column; align-items: center; gap: 24px; }
.nav-btn {
  background: rgba(237, 255, 249, 0.45); color: #2c3e32; border: none;
  width: 46px; height: 46px; border-radius: 14px; font-size: 22px;
  display: flex; align-items: center; justify-content: center; padding: 0; cursor: pointer; transition: all 0.2s;
}
.nav-btn:hover { transform: translateY(-2px); }
.nav-btn.active { background: rgba(237, 255, 249, 0.8); }

/* ============================================================ */
/* 3. 右侧主布局 (左右分栏) */
.main-content {
  position: absolute; top: 0; left: 100px; right: 0; bottom: 0;
  display: flex; flex-direction: column; padding: 30px 40px 20px 30px; box-sizing: border-box;
}
.content-grid { flex: 1; display: flex; gap: 24px; overflow: hidden; }

/* ============================================================ */
/* 4. 左半边 - 日历区 (占比33%) */
.calendar-card {
  width: 33%; height: 100%;
  background: rgba(245, 255, 253, 0.35);
  backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  border-radius: 16px; padding: 20px; box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex; flex-direction: column;
}

.month-nav {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-shrink: 0;
}
.month-title { font-size: 18px; font-weight: 600; color: rgb(51, 69, 64); }
.nav-btn-cal {
  background: none; border: none; font-size: 20px; cursor: pointer; color: rgb(51, 69, 64);
  padding: 0 12px; transition: 0.2s;
}
.nav-btn-cal:hover { transform: scale(1.2); }

.weekdays {
  display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-weight: 600; color: rgba(51, 69, 64, 0.7); font-size: 12px; margin-bottom: 8px;
}
.calendar-grid {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; flex: 1;
}

/* ✅ 关键修复：用 flex 代替 aspect-ratio 避免文字溢出 */
.calendar-day {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  border-radius: 8px; font-size: 13px; color: rgb(51, 69, 64); position: relative; cursor: default;
  min-height: 32px; /* 最小值 */
  padding: 4px 0;
}
.calendar-day.other-month { color: rgba(51, 69, 64, 0.3); }
.calendar-day.today { background: rgb(51, 69, 64); color: #fff; font-weight: 600; }
.calendar-day.has-event { font-weight: 600; }
.calendar-day.has-event:not(.today) { color: rgb(51, 69, 64); }
.calendar-day .event-dot { position: absolute; bottom: 2px; font-size: 8px; color: rgb(51, 69, 64); }
.calendar-day.today .event-dot { color: white; }

/* ============================================================ */
/* 5. 右半边 - 事件列表区 */
.event-panel {
  flex: 1; height: 100%; display: flex; flex-direction: column; gap: 14px;
}

/* 上：查询与筛选窄条 */
.event-tabs-bar {
  flex-shrink: 0; min-height: 56px;
  background: rgba(245, 255, 253, 0.35); backdrop-filter: blur(8px);
  border-radius: 16px; padding: 12px 24px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.filter-options { display: flex; gap: 8px; flex-wrap: wrap; }
.filter-btn {
  padding: 4px 14px; border: 1px solid rgba(51, 69, 64, 0.2); border-radius: 20px;
  background: transparent; cursor: pointer; font-size: 13px; color: rgb(51, 69, 64); transition: 0.2s;
}
.filter-btn:hover { background: rgba(51, 69, 64, 0.05); }
.filter-btn.active { background: rgb(51, 69, 64); color: #fff; border-color: rgb(51, 69, 64); }

.date-range-filter { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.date-range-filter input[type="date"] { padding: 4px 8px; border: 1px solid rgba(51, 69, 64, 0.2); border-radius: 8px; font-size: 13px; background: rgba(255,255,255,0.6); }
.clear-btn { background: none; border: none; cursor: pointer; font-size: 16px; color: #999; }

.add-event-btn {
  background: rgba(51, 69, 64, 0.8); border: none; border-radius: 20px; padding: 6px 16px; color: #fff; font-size: 14px; cursor: pointer; transition: 0.2s; white-space: nowrap;
}
.add-event-btn:hover { background: rgb(51, 69, 64); }

/* 下：内容大框 */
.event-content-box {
  flex: 1; overflow-y: auto; padding-right: 8px;
}

/* 单条事件样式 */
.event-item {
  background: rgba(245, 255, 253, 0.35); border-radius: 12px; padding: 16px 20px;
  display: flex; align-items: center; gap: 16px; margin-bottom: 14px; position: relative;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: 0.2s;
}
.event-item:hover { background: rgba(245, 255, 253, 0.6); }

.event-index { font-size: 18px; font-weight: 700; color: rgb(51, 69, 64); width: 28px; flex-shrink: 0; }
.event-info { flex: 1; cursor: pointer; }
.event-title { font-size: 16px; font-weight: 600; color: rgb(51, 69, 64); }
.event-desc { font-size: 13px; color: rgba(51, 69, 64, 0.7); margin-top: 4px; }
.event-status { font-size: 12px; margin-top: 6px; display: inline-block; padding: 2px 10px; border-radius: 12px; background: rgba(255,255,255,0.4); }

.status-pending { color: #2196F3; }
.status-progress { color: #4CAF50; }
.status-done { color: #9E9E9E; }
.status-canceled { color: #F44336; }

.event-actions { display: flex; gap: 6px; flex-shrink: 0; }
.edit-btn, .del-btn { background: none; border: none; cursor: pointer; font-size: 16px; padding: 4px; transition: 0.2s; }
.edit-btn:hover { transform: scale(1.1); }
.del-btn:hover { color: #d0314e; transform: scale(1.1); }

/* 线样式：两头渐变分割线 */
.event-item::after {
  content: ''; position: absolute; bottom: -7px; left: 0; width: 100%; height: 1px;
  background: linear-gradient(to right, rgba(51, 69, 64, 0), rgba(51, 69, 64, 0.4) 30%, rgba(51, 69, 64, 0.4) 70%, rgba(51, 69, 64, 0));
  pointer-events: none;
}
.event-item:last-child::after { display: none; }

.empty-state { text-align: center; color: rgba(51, 69, 64, 0.6); padding: 60px 0; font-size: 16px; }

/* ============================================================ */
/* 6. 弹窗样式 */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px); display: flex; justify-content: center; align-items: center; z-index: 1000;
}
.modal {
  background: rgba(255, 255, 255, 0.9); border-radius: 16px; padding: 24px; max-width: 400px; width: 90%; max-height: 80vh; overflow-y: auto;
}
.modal h2 { margin: 0 0 16px 0; font-size: 18px; color: rgb(51, 69, 64); }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 14px; font-weight: 500; color: rgb(51, 69, 64); margin-bottom: 4px; }
.form-group input, .form-group textarea, .form-group select {
  width: 100%; padding: 8px 12px; border: 1px solid rgba(51, 69, 64, 0.2); border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none;
}
.form-group input:focus, .form-group textarea:focus, .form-group select:focus { border-color: rgb(51, 69, 64); }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 16px; }
.cancel-btn { padding: 8px 20px; border: 1px solid rgba(51, 69, 64, 0.2); border-radius: 8px; background: white; cursor: pointer; color: #555; }
.save-btn { padding: 8px 20px; border: none; border-radius: 8px; background: rgb(51, 69, 64); color: white; cursor: pointer; }
.event-detail-item { padding: 6px 0; font-size: 14px; color: #333; }
.event-detail-item strong { color: rgb(51, 69, 64); }
</style>