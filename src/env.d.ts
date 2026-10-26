/// <reference types="vite/client" />

declare const __MAIAN_PREVIEW_PASSWORD__: string
declare const __MAIAN_PUBLIC_DEMO__: boolean
declare const __MAIAN_PREVIEW_LOCATION__: import('./utils/location').AppLocation | null

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
