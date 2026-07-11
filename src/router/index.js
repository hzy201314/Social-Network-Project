import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import ProfileView from '../views/ProfileView.vue'
import SearchView from '../views/SearchView.vue'
import FriendsView from '../views/FriendsView.vue'
import PublishView from '../views/PublishView.vue'
import ChatView from '../views/ChatView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import CalendarView from '../views/CalendarView.vue'
import GroupChatView from '../views/GroupChatView.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView
  },
  {
    path: '/home',
    name: 'home',
    component: HomeView
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView
  },
  {
    path: '/profile/:userId',
    name: 'profile-user',
    component: ProfileView
  },
  {
    path: '/search',
    name: 'search',
    component: SearchView
  },
  {
    path: '/friends',
    name: 'friends',
    component: FriendsView
  },
  {
    path: '/publish',
    name: 'publish',
    component: PublishView
  },
  {
    path: '/chat',
    name: 'chat',
    component: ChatView
  },
  {
    path: '/notifications',
    name: 'notifications',
    component: NotificationsView
  },
  {
    path: '/post/:postId',
    name: 'post-detail',
    component: () => import('@/views/PostDetailView.vue')
  },
  {
    path: '/calendar',
    name: 'calendar',
    component: CalendarView
  },
  {
    path: '/group-chat',
    name: 'group-chat',
    component: GroupChatView
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router