import { createRouter, createWebHistory } from 'vue-router'
import NotFoundView from '@/views/NotFoundView.vue'
import ProfessorFormView from '@/views/ProfessorFormView.vue'
import ProfessorListView from '@/views/ProfessorListView.vue'
import StudentFormView from '@/views/StudentFormView.vue'
import StudentListView from '@/views/StudentListView.vue'
import { focusPageHeading } from '@/utils/focus'

const APP_NAME = 'Bom Jesus - Challenge'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/students' },
    { path: '/students', component: StudentListView, meta: { title: 'Alunos' } },
    { path: '/students/new', component: StudentFormView, meta: { title: 'Novo aluno' } },
    { path: '/students/:id/edit', component: StudentFormView, meta: { title: 'Editar aluno' } },
    { path: '/professors', component: ProfessorListView, meta: { title: 'Professores' } },
    { path: '/professors/new', component: ProfessorFormView, meta: { title: 'Novo professor' } },
    {
      path: '/professors/:id/edit',
      component: ProfessorFormView,
      meta: { title: 'Editar professor' },
    },
    { path: '/:pathMatch(.*)*', component: NotFoundView, meta: { title: 'Página não encontrada' } },
  ],
})

// Após cada navegação: título da aba e foco no h1 da página (FR-025a)
router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? APP_NAME)} — ${APP_NAME}`
  focusPageHeading()
})

export default router
