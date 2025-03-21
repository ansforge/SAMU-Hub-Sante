import { defineNuxtPlugin } from 'nuxt/app'
import Vue3Toastify, { toast } from 'vue3-toastify'
import type { CSSTransitionProps } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css'

export default defineNuxtPlugin((nuxtApp) => {
  const customAnimation: CSSTransitionProps = {
    enter: 'Toastify--animate Toastify__none-enter--top-center',
    exit: 'd-none'
  }

  nuxtApp.vueApp.use(Vue3Toastify, {
    position: toast.POSITION.TOP_CENTER,
    autoClose: 5000,
    transition: customAnimation,
    dangerouslyHTMLString: true,
    newestOnTop: true
  })

  return {
    provide: { toast }
  }
})