<script setup lang="ts">
import DeviceList from "./components/DeviceList.vue"
import type { DeviceProps } from "./components/DeviceList.vue";
import { ref } from "vue"

let a = {
	"device": {
		"id": "abc1",
		"url": "http://xxx.iot.lan",
		"name": "xxx",
		"model": "MODEL-XXX",
		"manufacturer": "Shelly"
	},
	"version": "v1.14.0"
}
let b = {
	"device": {
		"id": "abc2",
		"url": "http://yyy.iot.lan",
		"name": "yyy",
		"nameByUser": "yyy-by-user",
		"model": "MODEL-YYY",
		"manufacturer": "Shelly"
	},
	"version": "v1.14.0"
}
const devices = ref({ devices: [ a, b ].map(x => Object.assign({}, x.device, {newVersion: x.version}) as DeviceProps) })

function testCallback(x: DeviceProps) {
	console.log("Update firmware callback", x)
}
</script>

<template>
	<DeviceList v-bind="devices" @update-firmware="testCallback" />
</template>

<style scoped>

</style>
