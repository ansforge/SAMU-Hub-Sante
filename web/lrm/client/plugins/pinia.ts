import { useMainStore } from '~/store'

export default ({ $pinia }: { $pinia: any }) => {
  return {
    provide: {
      store: useMainStore($pinia)
    }
  }
}
