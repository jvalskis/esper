<script setup lang="ts">
import Device from "./Device.vue"
import type { DeviceProps } from "./Device.vue";

defineProps<{
	devices: DeviceProps[]
}>()
</script>

<template>
    <div class="wrapper">
		<ui-list :type="2">
			<ui-list-group v-for="device in devices" :key="device">
				<Device v-bind="device" @action-button-clicked="$emit('action-button-clicked', $event)" />
			</ui-list-group>
		</ui-list>
	</div>
</template>

<style>
.button {
	text-transform: none !important;
}
.item-details {
	margin-right: 25px;
}
</style>