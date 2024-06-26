import { useMainStore } from '~/store'

export default defineNuxtPlugin (({ $pinia }: { $pinia: any }) => {
  return {
    provide: {
      store: useMainStore($pinia)
    }
  }
})
