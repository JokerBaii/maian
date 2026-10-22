/// <reference types="vite/client" />

declare const __MAIAN_PREVIEW_PASSWORD__: string

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
