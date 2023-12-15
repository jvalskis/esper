<script setup lang="ts">
import DeviceList from "./components/DeviceList.vue"
import type { DeviceProps } from "./components/Device.vue";
import { ref, onMounted } from "vue"
import { config } from './config'

import axios, { AxiosError } from 'axios'

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
        version: device.device.softwareVersion,
        displayStatus: "none"
     }) as DeviceProps
}

async function updateFirmware(device: DeviceProps) {
    if (device.displayStatus !== "progress") {
        device.displayStatus = "progress"
        let deviceInfo = { 
            deviceId: device.id,
            currentVersion: device.version, 
            newVersion: device.newVersion
        }
        console.log("Firmware update: start", deviceInfo)
        try {
			const result = await axios.get<GetDeviceUpdatesResponse>(config.ESPER_API_ADDRESS + "/devices/updates")
            console.log("Firmware update: completed", deviceInfo, result)
			setTimeout(() => {
				device.version = device.newVersion
				device.newVersion = undefined
				device.displayStatus = "none"
			}, 1000)
        } catch (error) {
            console.log("Firmware update: failed", deviceInfo, error)
			setTimeout(() => {
        		device.displayStatus = "error"
				if (axios.isAxiosError(error))  {
					const axiosError = error as AxiosError<Error>;
					device.error = axiosError.message
				}
			}, 1000)
        }
    } else {
        console.log("Firmware update: in progress")
    }
}
</script>

<template>
	<DeviceList :devices="data" @action-button-clicked="updateFirmware" />
</template>

<style scoped>

</style>
