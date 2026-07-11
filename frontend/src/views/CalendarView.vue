<template>
  <div class="calendar-container">
    <!-- 顶部导航 -->
    <div class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>📅 活动日历</h1>
      <button @click="showAddEvent = true" class="add-btn">+ 添加</button>
    </div>

    <!-- 缩小的日历 -->
    <div class="calendar-wrapper">
      <div class="month-nav">
        <button @click="prevMonth" class="nav-btn">‹</button>
        <span class="month-title">{{ currentYear }}年 {{ currentMonth }}月</span>
        <button @click="nextMonth" class="nav-btn">›</button>
      </div>

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

    <!-- ===== 筛选栏 ===== -->
    <div class="filter-bar">
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
      <div class="date-range-filter" v-if="currentFilter === 'dateRange'">
        <input type="date" v-model="dateRangeStart" @change="applyDateRangeFilter" />
        <span>至</span>
        <input type="date" v-model="dateRangeEnd" @change="applyDateRangeFilter" />
        <button @click="clearDateRange" class="clear-btn">✕</button>
      </div>
    </div>

    <!-- ===== 活动列表 ===== -->
    <div class="day-events">
      <div class="day-events-header">
        <h3>📋 活动列表</h3>
        <span class="event-count">{{ filteredEvents.length }} 项</span>
      </div>
      <div v-if="filteredEvents.length === 0" class="no-events">
        📭 暂无活动
      </div>
      <div v-else v-for="event in filteredEvents" :key="event.id" class="event-item">
        <div class="event-date">{{ formatDateShort(event.date) }}</div>
        <div class="event-time">{{ event.time || '全天' }}</div>
        <div class="event-content" @click="showEventDetailModal(event)">
          <div class="event-title">{{ event.title }}</div>
          <div class="event-desc">{{ event.description || '' }}</div>
          <div class="event-status" :class="getStatusClass(event.status)">
            {{ getStatusText(event.status) }}
          </div>
        </div>
        <div class="event-actions">
          <button @click="editEvent(event)" class="edit-event-btn">✏️</button>
          <button @click="deleteEvent(event.id)" class="delete-event-btn">✕</button>
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

    <!-- ===== 活动详情浮窗 ===== -->
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

// ===== 状态 =====
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const events = ref([])              // 所有日程
const weekdays = ['日', '一', '二', '三', '四', '五', '六']

// ===== 筛选 =====
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
  id: null,
  title: '',
  date: new Date().toISOString().split('T')[0],
  time: '',
  description: '',
  status: 0
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

  // 按状态筛选
  if (currentFilter.value === 'completed') {
    result = result.filter(e => e.status === 2)
  } else if (currentFilter.value === 'uncompleted') {
    result = result.filter(e => e.status !== 2 && e.status !== 3)
  } else if (currentFilter.value === 'all') {
    // 全部，不做筛选
  } else if (currentFilter.value === 'dateRange' && dateRangeStart.value && dateRangeEnd.value) {
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
        id: item.id,
        title: item.title,
        date: item.startTime ? item.startTime.split('T')[0] : '',
        time: item.startTime ? item.startTime.split('T')[1]?.slice(0, 5) : '',
        description: item.description || '',
        status: item.status !== undefined ? item.status : 0
      }))
    }
  } catch (error) {
    console.error('加载日程失败:', error)
    events.value = []
  }
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
  // 重新加载所有数据，然后 filteredEvents 会自动筛选
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
const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

// ===== 状态文本和样式 =====
const getStatusText = (status) => {
  const map = { 0: '待进行', 1: '进行中', 2: '已完成', 3: '已取消' }
  return map[status] || '待进行'
}

const getStatusClass = (status) => {
  const map = { 0: 'status-pending', 1: 'status-progress', 2: 'status-done', 3: 'status-canceled' }
  return map[status] || ''
}

// ===== 保存活动 =====
const saveEvent = async () => {
  if (!eventForm.value.title.trim()) {
    alert('请输入活动标题')
    return
  }
  if (!eventForm.value.date) {
    alert('请选择日期')
    return
  }

  const startDateTime = `${eventForm.value.date}T${eventForm.value.time || '00:00'}:00`
  const endDateTime = `${eventForm.value.date}T${eventForm.value.time ? String(Number(eventForm.value.time.split(':')[0]) + 1).padStart(2, '0') + ':' + eventForm.value.time.split(':')[1] : '01:00'}:00`

  const payload = {
    title: eventForm.value.title,
    description: eventForm.value.description || '',
    startTime: startDateTime,
    endTime: endDateTime,
    location: '',
    isAllDay: eventForm.value.time ? 0 : 1,
    remindMinutes: 30,
    participantIds: [],
    status: eventForm.value.status || 0
  }

  try {
    let res
    if (showEditEvent.value && eventForm.value.id) {
      res = await request.put(`/api/schedules/${eventForm.value.id}`, payload)
    } else {
      res = await request.post('/api/schedules', payload)
    }

    if (res.data.code === 0) {
      await loadEvents()
      closeModal()
      alert(showEditEvent.value ? '活动更新成功！' : '活动添加成功！')
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (error) {
    console.error('保存活动失败:', error)
    alert('保存失败，请重试')
  }
}

// ===== 编辑活动 =====
const editEvent = (event) => {
  eventForm.value = {
    id: event.id,
    title: event.title,
    date: event.date,
    time: event.time || '',
    description: event.description || '',
    status: event.status !== undefined ? event.status : 0
  }
  showEditEvent.value = true
}

// ===== 删除活动 =====
const deleteEvent = async (eventId) => {
  if (!confirm('确定要删除这个活动吗？')) return
  
  try {
    const res = await request.delete(`/api/schedules/${eventId}`)
    if (res.data.code === 0) {
      await loadEvents()
      alert('活动已删除')
    } else {
      alert(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除活动失败:', error)
    alert('删除失败，请重试')
  }
}

// ===== 弹窗控制 =====
const closeModal = () => {
  showAddEvent.value = false
  showEditEvent.value = false
  eventForm.value = {
    id: null,
    title: '',
    date: new Date().toISOString().split('T')[0],
    time: '',
    description: '',
    status: 0
  }
}

const showEventDetailModal = (event) => {
  selectedEventDetail.value = event
  showEventDetail.value = true
}

// ===== 格式化日期 =====
const formatDateShort = (dateStr) => {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return `${parts[0]}年${parseInt(parts[1])}月${parseInt(parts[2])}日`
}

// ===== 返回 =====
const goBack = () => router.push('/home')

// ===== 初始化 =====
onMounted(() => {
  loadEvents()
})
</script>

<style scoped>
.calendar-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 16px 80px 16px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 12px 0;
  border-bottom: 1px solid #e8ecf1;
}
.header h1 { font-size: 18px; font-weight: 600; margin: 0; }
.back-btn { background: none; border: none; font-size: 16px; cursor: pointer; color: #667eea; padding: 4px 8px; }
.add-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

/* ===== 缩小的日历 ===== */
.calendar-wrapper {
  background: white;
  border-radius: 12px;
  padding: 12px;
  margin-top: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.month-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 8px 0;
}
.month-title { font-size: 16px; font-weight: 600; color: #1f2a3a; }
.nav-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #667eea;
  padding: 0 12px;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  padding: 4px 0;
  font-weight: 600;
  color: #9aa6b5;
  font-size: 12px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}
.calendar-day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 13px;
  color: #1f2a3a;
  position: relative;
  cursor: default;
}
.calendar-day.other-month { color: #ccc; }
.calendar-day.today {
  background: #667eea;
  color: white;
  font-weight: 600;
}
.calendar-day.has-event { font-weight: 600; }
.calendar-day.has-event:not(.today) { color: #667eea; }
.calendar-day .event-dot {
  position: absolute;
  bottom: 2px;
  font-size: 6px;
  color: #667eea;
}
.calendar-day.today .event-dot { color: white; }
.calendar-day .day-number { font-size: 13px; }

/* ===== 筛选栏 ===== */
.filter-bar {
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  margin-top: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.filter-options {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.filter-btn {
  padding: 4px 12px;
  border: 1px solid #d0d7de;
  border-radius: 16px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #555;
  transition: all 0.2s;
}
.filter-btn:hover { background: #f0f2f5; }
.filter-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}
.date-range-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.date-range-filter input[type="date"] {
  padding: 4px 8px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  font-size: 13px;
}
.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #999;
  padding: 0 4px;
}
.clear-btn:hover { color: #ff4d4f; }

/* ===== 活动列表 ===== */
.day-events {
  background: white;
  border-radius: 12px;
  padding: 16px;
  margin-top: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  flex: 1;
}
.day-events-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.day-events-header h3 { font-size: 16px; margin: 0; color: #1f2a3a; }
.event-count { font-size: 13px; color: #9aa6b5; }

.no-events {
  text-align: center;
  color: #9aa6b5;
  padding: 20px 0;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f2f5;
}
.event-item:last-child { border-bottom: none; }

.event-date {
  font-size: 12px;
  color: #9aa6b5;
  min-width: 70px;
  flex-shrink: 0;
}
.event-time {
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
  min-width: 50px;
  flex-shrink: 0;
}
.event-content { flex: 1; cursor: pointer; }
.event-title { font-size: 15px; color: #1f2a3a; }
.event-desc { font-size: 13px; color: #9aa6b5; }
.event-status { font-size: 12px; margin-top: 2px; }
.status-pending { color: #2196F3; }
.status-progress { color: #4CAF50; }
.status-done { color: #9E9E9E; }
.status-canceled { color: #F44336; }

.event-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.edit-event-btn, .delete-event-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
}
.edit-event-btn:hover { opacity: 0.7; }
.delete-event-btn:hover { color: #ff4d4f; }

/* ===== 弹窗 ===== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal {
  background: white;
  border-radius: 16px;
  padding: 24px;
  max-width: 400px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}
.modal h2 { margin: 0 0 16px 0; font-size: 18px; }
.form-group { margin-bottom: 12px; }
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}
.form-group input, .form-group textarea, .form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-group input:focus, .form-group textarea:focus, .form-group select:focus {
  outline: none;
  border-color: #667eea;
}
.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 16px;
}
.cancel-btn {
  padding: 8px 20px;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  background: white;
  cursor: pointer;
}
.save-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
}

.event-detail-item { padding: 8px 0; font-size: 14px; color: #333; }
.event-detail-item strong { color: #1f2a3a; }
</style>