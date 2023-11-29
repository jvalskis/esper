export interface Config {
    ESPER_API_ADDRESS: string
}

export const config: Config = {
    ESPER_API_ADDRESS: import.meta.env.VITE_ESPER_API_ADDRESS,
}
