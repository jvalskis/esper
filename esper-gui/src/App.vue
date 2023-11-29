<script setup lang="ts">
import DeviceList from "./components/DeviceList.vue"
import type { DeviceProps } from "./components/DeviceList.vue";
import { ref, onMounted } from "vue"
import { config } from './config'

import axios from 'axios'

const data = ref(<DeviceProps[]>[])

type Device = {
    id: string,
    manufacturer: string,
    model: string,
    name: string,
    nameByUser?: string,
    softwareVersion: string,
    url: string,
}

type DeviceUpdate = {
    device: Device,
    version: string,
}

type GetDeviceUpdatesResponse = DeviceUpdate[]

onMounted(() => {
    console.log("ENV", import.meta.env)
	downloadDevices()
        .then((devices) => {
            data.value = devices.map(mapDeviceToProps)
        })
})

async function downloadDevices(): Promise<DeviceUpdate[]> {
	const result = await axios.get<GetDeviceUpdatesResponse>(config.ESPER_API_ADDRESS + "/devices/updates")
    return result.data
}

function mapDeviceToProps(device: DeviceUpdate): DeviceProps {
    return Object.assign({}, device.device, { 
        newVersion: device.version,
        version: device.device.softwareVersion
     }) as DeviceProps
}

function testCallback(x: DeviceProps) {
	console.log("Update firmware callback", x)
}

</script>

<template>
	<DeviceList :devices="data" @update-firmware="testCallback" />
</template>

<style scoped>

</style>
