import { createRouter, createWebHistory } from 'vue-router'
import LayoutView from '@/views/layout/index.vue';
import ChatView from '@/views/chat/index.vue';
import TestView from '@/views/test/index.vue';
// Feedback 已合并到综合评价报告页（/report/career?page=feedback）
import SkillReportView from '@/views/report/skill.vue';
import HollandReportView from '@/views/report/holland.vue';
import CareerReportView from '@/views/report/career.vue';
import ConfirmView from '@/views/confirm/index.vue';
import ConfirmQuickView from '@/views/confirm/quick.vue';

const enableConfirmQuick =
  import.meta.env.DEV || String(import.meta.env.VITE_ENABLE_CONFIRM_QUICK).toLowerCase() === 'true';

const confirmQuickRoute = enableConfirmQuick
  ? [
      {
        path: '/confirm-quick',
        name: 'confirmQuick',
        component: ConfirmQuickView,
      },
    ]
  : [];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/test/:type',
      name: 'test',
      component: TestView,
      props: true,
    },
    {
      path: '/feedback',
      redirect: '/report/career?page=feedback',
    },
    {
      path: '/report/skill',
      name: 'skillReport',
      component: SkillReportView,
    },
    {
      path: '/report/holland',
      name: 'hollandReport',
      component: HollandReportView,
    },
    {
      path: '/report/career',
      name: 'careerReport',
      component: CareerReportView,
    },
    {
      path: '/confirm',
      name: 'confirm',
      component: ConfirmView,
    },
    ...confirmQuickRoute,
    {
      path: '/',
      name: '',
      component: LayoutView,
      children: [
        {
          path: '/chat',
          name: 'chat',
          component: ChatView,
        },
        {
          path: '/',
          redirect: '/chat',
        },
      ],
    },
  ],
})

export default router
