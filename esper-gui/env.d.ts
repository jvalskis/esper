/// <reference types="vite/client" />

interface ImportMetaEnv {
	readonly VITE_ESPER_API_ADDRESS: string
}

interface ImportMeta {
	readonly env: ImportMetaEnv
}