<script setup lang="ts">
export interface DeviceProps {
	id: string,
	url: string,
	name: string,
	nameByUser?: string,
	model: string,
	manufacturer: string,
	version?: string,
	newVersion?: string,
	displayStatus: "none" | "progress" | "error",
	error?: string
}
const device = defineProps<DeviceProps>()

defineEmits(['actionButtonClicked'])
</script>

<template>
	<ui-item>
		<ui-item-text-content class="item-details">
			<ui-item-text1>{{ device.nameByUser ?? device.name }}</ui-item-text1>
			<ui-item-text2>{{ device.manufacturer }} / {{ device.model }}: {{ device.version ?? "Unknown version" }}</ui-item-text2>
		</ui-item-text-content>
		<ui-item-last-content v-if="device.newVersion">
			<ui-fab extended mini @click="$emit('actionButtonClicked', device)" class="button">
				<span>{{ device.newVersion }}</span>
				<template #before="{ iconClass }">
					<ui-icon :class="iconClass">upload</ui-icon>
				</template>
			</ui-fab>
		</ui-item-last-content>
	</ui-item>
	<figure v-if="device.displayStatus !== 'none'">
		<ui-progress :active="device.displayStatus === 'progress'" :progress="device.displayStatus === 'error' ? '1' : undefined"></ui-progress>
	</figure>
	<ui-alert state="error" v-if="device.displayStatus === 'error'">{{ device.error ?? "Error" }}</ui-alert>
</template>

<style>
.button {
	text-transform: none !important;
}
.item-details {
	margin-right: 25px;
}
.alert {
	margin-left: 16px;
	margin-right: 16px;
}
</style>