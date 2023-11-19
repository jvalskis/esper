import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'

import BalmUI from 'balm-ui';
import './styles/index.scss'

const app = createApp(App)

app.use(BalmUI);

app.mount('#app')